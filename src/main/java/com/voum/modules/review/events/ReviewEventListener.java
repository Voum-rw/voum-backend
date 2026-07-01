package com.voum.modules.review.events;

import com.voum.modules.review.service.TrustScoreService;
import com.voum.modules.trip.events.TripCancelledEvent;
import com.voum.modules.trip.events.TripCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final TrustScoreService trustScoreService;

    @EventListener
    public void onPassengerReviewedMotari(PassengerReviewedMotariEvent event) {
        log.info("Handling PassengerReviewedMotariEvent for Motari: {}", event.getReview().getReviewedUserId());
        trustScoreService.recalculateMotariMetrics(event.getReview().getReviewedUserId());
    }

    @EventListener
    public void onMotariReviewedPassenger(MotariReviewedPassengerEvent event) {
        log.info("Handling MotariReviewedPassengerEvent for Passenger: {}", event.getReview().getReviewedUserId());
        trustScoreService.recalculatePassengerMetrics(event.getReview().getReviewedUserId());
    }

    @EventListener
    public void onTripCompleted(TripCompletedEvent event) {
        log.info("Handling TripCompletedEvent for Motari: {} and Passenger: {}", event.getTrip().getMotariId(), event.getTrip().getPassengerId());
        trustScoreService.recalculateMotariMetrics(event.getTrip().getMotariId());
        trustScoreService.recalculatePassengerMetrics(event.getTrip().getPassengerId());
    }

    @EventListener
    public void onTripCancelled(TripCancelledEvent event) {
        log.info("Handling TripCancelledEvent for Motari: {} and Passenger: {}", event.getTrip().getMotariId(), event.getTrip().getPassengerId());
        trustScoreService.recalculateMotariMetrics(event.getTrip().getMotariId());
        trustScoreService.recalculatePassengerMetrics(event.getTrip().getPassengerId());
    }
}
