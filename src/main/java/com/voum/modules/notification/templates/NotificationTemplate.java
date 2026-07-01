package com.voum.modules.notification.templates;

import lombok.Getter;

/**
 * Notification templates mapping event types to push notification titles and body text.
 * All 12 notification types for Sprint 8.
 */
@Getter
public enum NotificationTemplate {

    // ── Marketplace ──────────────────────────────────────────────────────────
    RIDE_REQUEST_NEARBY(
            "New Passenger Nearby",
            "A passenger is requesting a ride near your location."
    ),
    NEW_OFFER_RECEIVED(
            "New Offer Received",
            "A Motari has made an offer for your ride request."
    ),
    OFFER_ACCEPTED(
            "Offer Accepted",
            "Your offer was accepted. Get ready to pick up your passenger!"
    ),
    RIDE_REQUEST_EXPIRED(
            "Request Expired",
            "No driver accepted your request in time. Please try again."
    ),

    // ── Trip Lifecycle ────────────────────────────────────────────────────────
    TRIP_CREATED(
            "Trip Created",
            "Your trip has been created. Your Motari is on the way."
    ),
    MOTARI_EN_ROUTE(
            "Motari En Route",
            "Your Motari is on their way to pick you up."
    ),
    MOTARI_ARRIVED(
            "Your Motari Has Arrived",
            "Your Motari has arrived at the pickup location."
    ),
    TRIP_STARTED(
            "Trip Started",
            "Your trip has started. Enjoy the ride!"
    ),
    TRIP_COMPLETED(
            "Trip Completed",
            "Your trip has been completed. Thank you for using Voum!"
    ),
    TRIP_CANCELLED(
            "Trip Cancelled",
            "Your trip has been cancelled."
    ),

    // ── Account ───────────────────────────────────────────────────────────────
    ACCOUNT_APPROVED(
            "Account Approved",
            "Congratulations! Your Voum driver account has been approved. You can now start accepting rides."
    ),
    ACCOUNT_REJECTED(
            "Account Rejected",
            "Your Voum driver account application was not approved. Please review the details and resubmit."
    ),

    // ── Reviews & Trust ───────────────────────────────────────────────────────
    NEW_REVIEW_RECEIVED(
            "New Review Received",
            "A user left you a new rating and comment."
    ),
    REVIEW_SUBMITTED_CONFIRMATION(
            "Review Submitted",
            "Thank you! Your review has been recorded successfully."
    ),
    TRUST_SCORE_UPDATED(
            "Trust Score Updated",
            "Your driver trust score has been updated."
    );


    private final String title;
    private final String body;

    NotificationTemplate(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
