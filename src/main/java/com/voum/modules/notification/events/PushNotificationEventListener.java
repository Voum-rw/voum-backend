package com.voum.modules.notification.events;

import com.voum.modules.marketplace.events.*;
import com.voum.modules.notification.service.PushNotificationService;
import com.voum.modules.notification.templates.NotificationTemplate;
import com.voum.modules.onboarding.events.AccountApprovedEvent;
import com.voum.modules.onboarding.events.AccountRejectedEvent;
import com.voum.modules.review.events.*;
import com.voum.modules.trip.events.*;
import com.voum.modules.support.events.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to all domain events and routes them to PushNotificationService.
 * All handlers are async so they don't block the main event thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationEventListener {

    private final PushNotificationService pushNotificationService;

    // ── Marketplace Events ────────────────────────────────────────────────────

    /**
     * Passenger created a ride request → notify nearby Motaris.
     * Note: Since we don't know nearby Motaris at event time (they're selected
     * by geo-query in the marketplace), we skip proactive notification here.
     * Motaris receive the request via WebSocket. This is a no-op for now.
     */
    @Async
    @EventListener
    public void onRideRequestCreated(RideRequestCreatedEvent event) {
        // Motaris are notified via WebSocket broadcast (marketplace engine).
        // Push notification to nearby Motaris requires the list of Motari IDs,
        // which is not available in this event. Left for future Sprint when
        // targeted push-to-Motari list is supported.
        log.debug("RideRequestCreatedEvent received – Motari push via WebSocket only for now.");
    }

    /**
     * A Motari submitted an offer → notify the passenger.
     */
    @Async
    @EventListener
    public void onRideOfferSubmitted(RideOfferSubmittedEvent event) {
        UUID passengerId = event.getPassengerId();

        Map<String, String> data = new HashMap<>();
        data.put("requestId", event.getRideOffer().getRideRequestId().toString());
        data.put("offerId", event.getRideOffer().getId().toString());

        pushNotificationService.sendPush(passengerId, NotificationTemplate.NEW_OFFER_RECEIVED, data);
    }

    /**
     * Passenger accepted an offer → notify the Motari.
     */
    @Async
    @EventListener
    public void onRideOfferAccepted(RideOfferAcceptedEvent event) {
        UUID motariUserId = event.getRideOffer().getMotariId(); // Motari ID == User ID
        Map<String, String> data = new HashMap<>();
        data.put("requestId", event.getRideRequest().getId().toString());
        data.put("offerId", event.getRideOffer().getId().toString());

        pushNotificationService.sendPush(motariUserId, NotificationTemplate.OFFER_ACCEPTED, data);
    }

    /**
     * Ride request expired without any driver accepting → notify the passenger.
     */
    @Async
    @EventListener
    public void onRideRequestExpired(RideRequestExpiredEvent event) {
        UUID passengerId = event.getRideRequest().getPassengerId();
        Map<String, String> data = new HashMap<>();
        data.put("requestId", event.getRideRequest().getId().toString());

        pushNotificationService.sendPush(passengerId, NotificationTemplate.RIDE_REQUEST_EXPIRED, data);
    }

    // ── Trip Events ──────────────────────────────────────────────────────────

    /**
     * Trip created → notify both passenger and Motari.
     */
    @Async
    @EventListener
    public void onTripCreated(TripCreatedEvent event) {
        String tripId = event.getTrip().getId().toString();
        Map<String, String> data = new HashMap<>();
        data.put("tripId", tripId);

        pushNotificationService.sendPush(event.getTrip().getPassengerId(), NotificationTemplate.TRIP_CREATED, data);
        pushNotificationService.sendPush(event.getTrip().getMotariId(), NotificationTemplate.TRIP_CREATED, data);
    }

    /**
     * Motari is en route → notify passenger.
     */
    @Async
    @EventListener
    public void onMotariEnRoute(MotariEnRouteEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("tripId", event.getTrip().getId().toString());

        pushNotificationService.sendPush(event.getTrip().getPassengerId(), NotificationTemplate.MOTARI_EN_ROUTE, data);
    }

    /**
     * Motari arrived at pickup → notify passenger.
     */
    @Async
    @EventListener
    public void onMotariArrived(MotariArrivedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("tripId", event.getTrip().getId().toString());

        pushNotificationService.sendPush(event.getTrip().getPassengerId(), NotificationTemplate.MOTARI_ARRIVED, data);
    }

    /**
     * Trip started → notify passenger.
     */
    @Async
    @EventListener
    public void onTripStarted(TripStartedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("tripId", event.getTrip().getId().toString());

        pushNotificationService.sendPush(event.getTrip().getPassengerId(), NotificationTemplate.TRIP_STARTED, data);
    }

    /**
     * Trip completed → notify both passenger and Motari.
     */
    @Async
    @EventListener
    public void onTripCompleted(TripCompletedEvent event) {
        String tripId = event.getTrip().getId().toString();
        Map<String, String> data = new HashMap<>();
        data.put("tripId", tripId);

        pushNotificationService.sendPush(event.getTrip().getPassengerId(), NotificationTemplate.TRIP_COMPLETED, data);
        pushNotificationService.sendPush(event.getTrip().getMotariId(), NotificationTemplate.TRIP_COMPLETED, data);
    }

    /**
     * Trip cancelled → notify both passenger and Motari.
     */
    @Async
    @EventListener
    public void onTripCancelled(TripCancelledEvent event) {
        String tripId = event.getTrip().getId().toString();
        Map<String, String> data = new HashMap<>();
        data.put("tripId", tripId);

        pushNotificationService.sendPush(event.getTrip().getPassengerId(), NotificationTemplate.TRIP_CANCELLED, data);
        pushNotificationService.sendPush(event.getTrip().getMotariId(), NotificationTemplate.TRIP_CANCELLED, data);
    }

    // ── Account Events ───────────────────────────────────────────────────────

    /**
     * Motari account approved → notify the Motari.
     */
    @Async
    @EventListener
    public void onAccountApproved(AccountApprovedEvent event) {
        pushNotificationService.sendPush(
                event.getMotariUserId(),
                NotificationTemplate.ACCOUNT_APPROVED,
                new HashMap<>()
        );
    }

    /**
     * Motari account rejected → notify the Motari.
     */
    @Async
    @EventListener
    public void onAccountRejected(AccountRejectedEvent event) {
        Map<String, String> data = new HashMap<>();
        if (event.getRejectionReason() != null) {
            data.put("reason", event.getRejectionReason());
        }
        pushNotificationService.sendPush(
                event.getMotariUserId(),
                NotificationTemplate.ACCOUNT_REJECTED,
                data
        );
    }

    // ── Reviews & Trust Events ───────────────────────────────────────────────

    /**
     * Passenger reviewed Motari → notify Motari (new review) and Passenger (confirm).
     */
    @Async
    @EventListener
    public void onPassengerReviewedMotari(PassengerReviewedMotariEvent event) {
        String tripId = event.getReview().getTripId().toString();
        Map<String, String> data = new HashMap<>();
        data.put("tripId", tripId);
        data.put("rating", event.getReview().getRating().toString());

        // Notify reviewed user (Motari)
        pushNotificationService.sendPush(event.getReview().getReviewedUserId(), NotificationTemplate.NEW_REVIEW_RECEIVED, data);

        // Notify reviewer (Passenger)
        pushNotificationService.sendPush(event.getReview().getReviewerId(), NotificationTemplate.REVIEW_SUBMITTED_CONFIRMATION, data);
    }

    /**
     * Motari reviewed Passenger → notify Passenger (new review) and Motari (confirm).
     */
    @Async
    @EventListener
    public void onMotariReviewedPassenger(MotariReviewedPassengerEvent event) {
        String tripId = event.getReview().getTripId().toString();
        Map<String, String> data = new HashMap<>();
        data.put("tripId", tripId);
        data.put("rating", event.getReview().getRating().toString());

        // Notify reviewed user (Passenger)
        pushNotificationService.sendPush(event.getReview().getReviewedUserId(), NotificationTemplate.NEW_REVIEW_RECEIVED, data);

        // Notify reviewer (Motari)
        pushNotificationService.sendPush(event.getReview().getReviewerId(), NotificationTemplate.REVIEW_SUBMITTED_CONFIRMATION, data);
    }

    /**
     * Trust score updated significantly (>= 1.0) → notify Motari.
     */
    @Async
    @EventListener
    public void onTrustScoreUpdated(TrustScoreUpdatedEvent event) {
        double diff = Math.abs(event.getNewScore() - event.getOldScore());
        if (diff >= 1.0) {
            Map<String, String> data = new HashMap<>();
            data.put("trustScore", String.format("%.2f", event.getNewScore()));
            pushNotificationService.sendPush(event.getMotariId(), NotificationTemplate.TRUST_SCORE_UPDATED, data);
        }
    }

    // ── Support & Dispute Events ─────────────────────────────────────────────

    @Async
    @EventListener
    public void onSupportTicketCreated(SupportTicketCreatedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", event.getTicket().getId().toString());
        data.put("type", event.getTicket().getType().name());
        pushNotificationService.sendPush(event.getTicket().getUserId(), NotificationTemplate.SUPPORT_TICKET_CREATED, data);
    }

    @Async
    @EventListener
    public void onSupportTicketAssigned(SupportTicketAssignedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", event.getTicket().getId().toString());
        pushNotificationService.sendPush(event.getTicket().getUserId(), NotificationTemplate.SUPPORT_REPLY_RECEIVED, data);
    }

    @Async
    @EventListener
    public void onSupportTicketClosed(SupportTicketClosedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("ticketId", event.getTicket().getId().toString());
        pushNotificationService.sendPush(event.getTicket().getUserId(), NotificationTemplate.SUPPORT_TICKET_CLOSED, data);
    }

    @Async
    @EventListener
    public void onUserReported(UserReportedEvent event) {
        if (event.getReport().getReporterId() != null) {
            Map<String, String> data = new HashMap<>();
            data.put("reportId", event.getReport().getId().toString());
            pushNotificationService.sendPush(event.getReport().getReporterId(), NotificationTemplate.REPORT_RECEIVED, data);
        }
    }

    @Async
    @EventListener
    public void onLostItemCreated(LostItemCreatedEvent event) {
        if (event.getLostItem().getReportedBy() != null) {
            Map<String, String> data = new HashMap<>();
            data.put("lostItemId", event.getLostItem().getId().toString());
            pushNotificationService.sendPush(event.getLostItem().getReportedBy(), NotificationTemplate.LOST_ITEM_UPDATED, data);
        }
    }
}

