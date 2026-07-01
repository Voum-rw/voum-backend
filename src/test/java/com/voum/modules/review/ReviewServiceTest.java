package com.voum.modules.review;

import com.voum.common.ApiException;
import com.voum.modules.users.Motari;
import com.voum.modules.users.Passenger;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.users.PassengerRepository;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.repository.TripRepository;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.dto.RideOfferResponse;
import com.voum.modules.marketplace.service.RideOfferService;
import com.voum.modules.marketplace.repository.RideOfferRepository;
import com.voum.modules.review.dto.MotariRatingResponse;
import com.voum.modules.review.dto.MotariReviewResponse;
import com.voum.modules.review.dto.ReviewRequest;
import com.voum.modules.review.entity.TripReview;
import com.voum.modules.review.repository.TripReviewRepository;
import com.voum.modules.review.service.ReviewService;
import com.voum.modules.review.service.TrustScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripReviewRepository tripReviewRepository;

    @Mock
    private MotariRepository motariRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private RideOfferRepository rideOfferRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReviewService reviewService;
    private TrustScoreService trustScoreService;

    private final UUID passengerId = UUID.randomUUID();
    private final UUID motariId = UUID.randomUUID();
    private final UUID tripId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        reviewService = new ReviewService(
                tripRepository,
                tripReviewRepository,
                motariRepository,
                eventPublisher
        );

        trustScoreService = new TrustScoreService(
                motariRepository,
                passengerRepository,
                tripRepository,
                rideOfferRepository,
                tripReviewRepository,
                eventPublisher
        );
    }

    // ── Review Eligibility & Validation Tests ─────────────────────────────────

    @Test
    public void submitPassengerReview_tripNotCompleted_shouldThrowException() {
        Trip trip = Trip.builder()
                .id(tripId)
                .status("IN_PROGRESS")
                .passengerId(passengerId)
                .motariId(motariId)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        ReviewRequest request = ReviewRequest.builder()
                .tripId(tripId)
                .rating(5)
                .comment("Great!")
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                reviewService.submitPassengerReview(passengerId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("completed trips"));
    }


    @Test
    public void submitPassengerReview_wrongPassenger_shouldThrowException() {
        Trip trip = Trip.builder()
                .id(tripId)
                .status("COMPLETED")
                .passengerId(UUID.randomUUID()) // different passenger
                .motariId(motariId)
                .completedAt(Instant.now())
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        ReviewRequest request = ReviewRequest.builder()
                .tripId(tripId)
                .rating(5)
                .comment("Great!")
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                reviewService.submitPassengerReview(passengerId, request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getMessage().contains("Only the passenger assigned"));
    }

    @Test
    public void submitPassengerReview_outside30DayWindow_shouldThrowException() {
        Trip trip = Trip.builder()
                .id(tripId)
                .status("COMPLETED")
                .passengerId(passengerId)
                .motariId(motariId)
                .completedAt(Instant.now().minus(31, ChronoUnit.DAYS)) // older than 30 days
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        ReviewRequest request = ReviewRequest.builder()
                .tripId(tripId)
                .rating(5)
                .comment("Great!")
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                reviewService.submitPassengerReview(passengerId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("review window"));
    }

    @Test
    public void submitPassengerReview_duplicateReview_shouldThrowException() {
        Trip trip = Trip.builder()
                .id(tripId)
                .status("COMPLETED")
                .passengerId(passengerId)
                .motariId(motariId)
                .completedAt(Instant.now())
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripReviewRepository.existsByTripIdAndReviewerId(tripId, passengerId)).thenReturn(true);

        ReviewRequest request = ReviewRequest.builder()
                .tripId(tripId)
                .rating(5)
                .comment("Great!")
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                reviewService.submitPassengerReview(passengerId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("already submitted"));
    }

    @Test
    public void submitPassengerReview_invalidRating_shouldThrowException() {
        ReviewRequest request = ReviewRequest.builder()
                .tripId(tripId)
                .rating(6) // invalid
                .comment("Too high")
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                reviewService.submitPassengerReview(passengerId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void submitPassengerReview_successful_shouldSaveReviewAndPublishEvent() {
        Trip trip = Trip.builder()
                .id(tripId)
                .status("COMPLETED")
                .passengerId(passengerId)
                .motariId(motariId)
                .completedAt(Instant.now())
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripReviewRepository.existsByTripIdAndReviewerId(tripId, passengerId)).thenReturn(false);
        when(tripReviewRepository.save(any(TripReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewRequest request = ReviewRequest.builder()
                .tripId(tripId)
                .rating(5)
                .comment("Excellent driver!")
                .build();

        reviewService.submitPassengerReview(passengerId, request);

        ArgumentCaptor<TripReview> captor = ArgumentCaptor.forClass(TripReview.class);
        verify(tripReviewRepository).save(captor.capture());

        TripReview saved = captor.getValue();
        assertEquals(tripId, saved.getTripId());
        assertEquals(passengerId, saved.getReviewerId());
        assertEquals(motariId, saved.getReviewedUserId());
        assertEquals(5, saved.getRating());
        assertEquals("Excellent driver!", saved.getComment());
        assertFalse(saved.isFlagged());
        assertEquals(1, saved.getReviewVersion());

        verify(eventPublisher, times(1)).publishEvent(any());
    }

    // ── Trust Score Calculation & Bayesian Blending Tests ────────────────────

    @Test
    public void recalculateMotariMetrics_newDriver_shouldEvaluateToNeutralFifty() {
        Motari motari = Motari.builder()
                .id(motariId)
                .totalReviews(0)
                .trustScore(100.0) // start at 100 before reset
                .build();

        when(motariRepository.findById(motariId)).thenReturn(Optional.of(motari));
        when(tripReviewRepository.getAverageRatingForUser(motariId)).thenReturn(null);
        when(tripReviewRepository.countReviewsForUser(motariId)).thenReturn(0);

        // Terminal stats
        when(tripRepository.countByMotariIdAndStatus(motariId, "COMPLETED")).thenReturn(0L);
        when(tripRepository.countByMotariIdAndStatusAndCancelledBy(motariId, "CANCELLED", motariId)).thenReturn(0L);
        when(tripRepository.countTerminalTripsForMotari(motariId)).thenReturn(0L);

        // Offer stats
        when(rideOfferRepository.countByMotariId(motariId)).thenReturn(0L);
        when(rideOfferRepository.countByMotariIdAndStatus(motariId, "ACCEPTED")).thenReturn(0L);

        trustScoreService.recalculateMotariMetrics(motariId);

        // Verify values
        assertNull(motari.getAverageRating());
        assertEquals(0, motari.getTotalReviews());
        assertEquals(100.0, motari.getCompletionRate());
        assertEquals(100.0, motari.getAcceptanceRate());
        assertEquals(0.0, motari.getCancellationRate());
        assertEquals(0, motari.getTotalCompletedTrips());
        
        // Bayesian blended trust score: confidence = 0.0 -> blended score = 50.00
        assertEquals(50.00, motari.getTrustScore());
    }

    @Test
    public void recalculateMotariMetrics_withReviews_shouldApplyBayesianBlending() {
        Motari motari = Motari.builder()
                .id(motariId)
                .build();

        when(motariRepository.findById(motariId)).thenReturn(Optional.of(motari));
        // Motari has 10 reviews with 5.0 rating average
        when(tripReviewRepository.getAverageRatingForUser(motariId)).thenReturn(5.0);
        when(tripReviewRepository.countReviewsForUser(motariId)).thenReturn(10);

        // Motari has 10 completed, 2 cancelled by Motari (total 12 terminal)
        when(tripRepository.countByMotariIdAndStatus(motariId, "COMPLETED")).thenReturn(10L);
        when(tripRepository.countByMotariIdAndStatusAndCancelledBy(motariId, "CANCELLED", motariId)).thenReturn(2L);
        when(tripRepository.countTerminalTripsForMotari(motariId)).thenReturn(12L);

        // Offers: 20 submitted, 10 accepted
        when(rideOfferRepository.countByMotariId(motariId)).thenReturn(20L);
        when(rideOfferRepository.countByMotariIdAndStatus(motariId, "ACCEPTED")).thenReturn(10L);

        trustScoreService.recalculateMotariMetrics(motariId);

        assertEquals(5.0, motari.getAverageRating());
        assertEquals(10, motari.getTotalReviews());
        assertEquals(10, motari.getTotalCompletedTrips());
        assertEquals((10.0 / 12.0) * 100.0, motari.getCompletionRate(), 0.01);
        assertEquals((2.0 / 12.0) * 100.0, motari.getCancellationRate(), 0.01);
        assertEquals(50.0, motari.getAcceptanceRate());

        // Trace trust score:
        // ratingQuality = 5.0 * 20.0 = 100.0
        // completionRate = 83.33
        // cancellationRate = 16.67 -> cancellationPart = 83.33
        // baseScore = (100.0 * 0.70) + (83.333 * 0.20) + (83.333 * 0.10)
        //           = 70.0 + 16.666 + 8.333 = 95.0
        // confidence = 10 / 50.0 = 0.20
        // trustScore = (95.0 * 0.20) + (50.0 * 0.80) = 19.0 + 40.0 = 59.00
        assertEquals(59.00, motari.getTrustScore(), 0.01);
    }
}
