package com.voum.modules.review.service;

import com.voum.common.ApiException;
import com.voum.modules.users.Motari;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.repository.TripRepository;
import com.voum.modules.review.dto.MotariRatingResponse;
import com.voum.modules.review.dto.MotariReviewResponse;
import com.voum.modules.review.dto.ReviewRequest;
import com.voum.modules.review.entity.TripReview;
import com.voum.modules.review.repository.TripReviewRepository;
import com.voum.modules.review.events.PassengerReviewedMotariEvent;
import com.voum.modules.review.events.MotariReviewedPassengerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final TripRepository tripRepository;
    private final TripReviewRepository tripReviewRepository;
    private final MotariRepository motariRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void submitPassengerReview(UUID callerPassengerId, ReviewRequest req) {
        log.info("Passenger {} submitting review for trip {}", callerPassengerId, req.getTripId());

        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new ApiException("Rating must be between 1 and 5.", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(req.getTripId())
                .orElseThrow(() -> new ApiException("Trip not found.", HttpStatus.NOT_FOUND));

        if (!"COMPLETED".equals(trip.getStatus())) {
            throw new ApiException("Reviews are only allowed for completed trips.", HttpStatus.BAD_REQUEST);
        }

        if (!trip.getPassengerId().equals(callerPassengerId)) {
            throw new ApiException("Access Denied: Only the passenger assigned to this trip can submit this review.", HttpStatus.FORBIDDEN);
        }

        // Self-review block (precautionary)
        if (callerPassengerId.equals(trip.getMotariId())) {
            throw new ApiException("Self-reviews are not allowed.", HttpStatus.BAD_REQUEST);
        }

        // Review Window (30 days)
        Instant completedAt = trip.getCompletedAt();
        if (completedAt == null || completedAt.isBefore(Instant.now().minus(30, ChronoUnit.DAYS))) {
            throw new ApiException("The 30-day review window for this trip has expired.", HttpStatus.BAD_REQUEST);
        }

        // Duplicate prevention
        if (tripReviewRepository.existsByTripIdAndReviewerId(trip.getId(), callerPassengerId)) {
            throw new ApiException("You have already submitted a review for this trip.", HttpStatus.BAD_REQUEST);
        }

        TripReview review = TripReview.builder()
                .tripId(trip.getId())
                .reviewerId(callerPassengerId)
                .reviewedUserId(trip.getMotariId())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        review = tripReviewRepository.save(review);
        log.info("Passenger review saved successfully: {}", review.getId());

        // Publish event
        eventPublisher.publishEvent(new PassengerReviewedMotariEvent(this, review));
    }

    @Transactional
    public void submitMotariReview(UUID callerMotariId, ReviewRequest req) {
        log.info("Motari {} submitting review for trip {}", callerMotariId, req.getTripId());

        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new ApiException("Rating must be between 1 and 5.", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(req.getTripId())
                .orElseThrow(() -> new ApiException("Trip not found.", HttpStatus.NOT_FOUND));

        if (!"COMPLETED".equals(trip.getStatus())) {
            throw new ApiException("Reviews are only allowed for completed trips.", HttpStatus.BAD_REQUEST);
        }

        if (!trip.getMotariId().equals(callerMotariId)) {
            throw new ApiException("Access Denied: Only the Motari assigned to this trip can submit this review.", HttpStatus.FORBIDDEN);
        }

        // Self-review block
        if (callerMotariId.equals(trip.getPassengerId())) {
            throw new ApiException("Self-reviews are not allowed.", HttpStatus.BAD_REQUEST);
        }

        // Review Window (30 days)
        Instant completedAt = trip.getCompletedAt();
        if (completedAt == null || completedAt.isBefore(Instant.now().minus(30, ChronoUnit.DAYS))) {
            throw new ApiException("The 30-day review window for this trip has expired.", HttpStatus.BAD_REQUEST);
        }

        // Duplicate prevention
        if (tripReviewRepository.existsByTripIdAndReviewerId(trip.getId(), callerMotariId)) {
            throw new ApiException("You have already submitted a review for this trip.", HttpStatus.BAD_REQUEST);
        }

        TripReview review = TripReview.builder()
                .tripId(trip.getId())
                .reviewerId(callerMotariId)
                .reviewedUserId(trip.getPassengerId())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        review = tripReviewRepository.save(review);
        log.info("Motari review saved successfully: {}", review.getId());

        // Publish event
        eventPublisher.publishEvent(new MotariReviewedPassengerEvent(this, review));
    }

    @Transactional(readOnly = true)
    public MotariRatingResponse getMotariRating(UUID motariId) {
        Motari motari = motariRepository.findById(motariId)
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        return MotariRatingResponse.builder()
                .averageRating(motari.getAverageRating())
                .totalReviews(motari.getTotalReviews())
                .completionRate(motari.getCompletionRate())
                .trustScore(motari.getTrustScore())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MotariReviewResponse> getMotariReviews(UUID motariId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TripReview> reviewPage = tripReviewRepository.findByReviewedUserId(motariId, pageable);

        return reviewPage.map(review -> MotariReviewResponse.builder()
                .id(review.getId())
                .tripId(review.getTripId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build());
    }
}
