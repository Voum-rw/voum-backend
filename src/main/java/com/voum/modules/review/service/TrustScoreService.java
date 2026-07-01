package com.voum.modules.review.service;

import com.voum.modules.users.Motari;
import com.voum.modules.users.Passenger;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.users.PassengerRepository;
import com.voum.modules.trip.repository.TripRepository;
import com.voum.modules.marketplace.repository.RideOfferRepository;
import com.voum.modules.review.repository.TripReviewRepository;
import com.voum.modules.review.events.TrustScoreUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustScoreService {

    private final MotariRepository motariRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final RideOfferRepository rideOfferRepository;
    private final TripReviewRepository tripReviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void recalculateMotariMetrics(UUID motariId) {
        Motari motari = motariRepository.findById(motariId).orElse(null);
        if (motari == null) return;

        // 1. Recalculate average rating & total reviews
        Double avgRating = tripReviewRepository.getAverageRatingForUser(motariId);
        Integer reviewCount = tripReviewRepository.countReviewsForUser(motariId);

        motari.setAverageRating(avgRating); // starts as NULL if no ratings exist
        motari.setTotalReviews(reviewCount != null ? reviewCount : 0);

        // 2. Recalculate completion and cancellation rates
        long completed = tripRepository.countByMotariIdAndStatus(motariId, "COMPLETED");
        long cancelledByMotari = tripRepository.countByMotariIdAndStatusAndCancelledBy(motariId, "CANCELLED", motariId);
        long totalTerminal = tripRepository.countTerminalTripsForMotari(motariId);

        double completionRate = 100.00;
        double cancellationRate = 0.00;

        if (totalTerminal > 0) {
            completionRate = ((double) completed / totalTerminal) * 100.0;
            cancellationRate = ((double) cancelledByMotari / totalTerminal) * 100.0;
        }

        motari.setCompletionRate(completionRate);
        motari.setCancellationRate(cancellationRate);
        motari.setTotalCompletedTrips((int) completed);

        // 3. Recalculate acceptance rate
        long totalOffers = rideOfferRepository.countByMotariId(motariId);
        long acceptedOffers = rideOfferRepository.countByMotariIdAndStatus(motariId, "ACCEPTED");

        double acceptanceRate = 100.00;
        if (totalOffers > 0) {
            acceptanceRate = ((double) acceptedOffers / totalOffers) * 100.0;
        }
        motari.setAcceptanceRate(acceptanceRate);

        // 4. Bayesian confidence blended trust score
        double oldScore = motari.getTrustScore() != null ? motari.getTrustScore() : 50.00;

        double confidence = Math.min((double) motari.getTotalReviews() / 50.0, 1.0);
        double ratingQuality = (motari.getAverageRating() != null ? motari.getAverageRating() : 2.5) * 20.0;

        double baseScore = (ratingQuality * 0.70) + (completionRate * 0.20) + ((100.0 - cancellationRate) * 0.10);
        double newScore = (baseScore * confidence) + (50.0 * (1.0 - confidence));

        newScore = Math.max(0.0, Math.min(100.0, newScore));
        motari.setTrustScore(newScore);

        motariRepository.save(motari);
        log.info("Recalculated metrics for Motari {}: avgRating={}, totalReviews={}, completionRate={}, cancellationRate={}, acceptanceRate={}, trustScore={}",
                motariId, motari.getAverageRating(), motari.getTotalReviews(), completionRate, cancellationRate, acceptanceRate, newScore);

        // 5. Publish Event
        eventPublisher.publishEvent(new TrustScoreUpdatedEvent(this, motariId, oldScore, newScore));
    }

    @Transactional
    public void recalculatePassengerMetrics(UUID passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId).orElse(null);
        if (passenger == null) return;

        // 1. Recalculate average rating & total reviews
        Double avgRating = tripReviewRepository.getAverageRatingForUser(passengerId);
        Integer reviewCount = tripReviewRepository.countReviewsForUser(passengerId);

        passenger.setAverageRating(avgRating); // starts as NULL if no ratings exist
        passenger.setTotalReviews(reviewCount != null ? reviewCount : 0);

        // 2. Recalculate cancellation rate
        long completed = tripRepository.countByPassengerIdAndStatus(passengerId, "COMPLETED");
        long cancelledByPassenger = tripRepository.countByPassengerIdAndStatusAndCancelledBy(passengerId, "CANCELLED", passengerId);
        long totalTerminal = tripRepository.countTerminalTripsForPassenger(passengerId);

        double cancellationRate = 0.00;
        if (totalTerminal > 0) {
            cancellationRate = ((double) cancelledByPassenger / totalTerminal) * 100.0;
        }
        passenger.setCancellationRate(cancellationRate);

        passengerRepository.save(passenger);
        log.info("Recalculated metrics for Passenger {}: avgRating={}, totalReviews={}, cancellationRate={}",
                passengerId, passenger.getAverageRating(), passenger.getTotalReviews(), cancellationRate);
    }
}
