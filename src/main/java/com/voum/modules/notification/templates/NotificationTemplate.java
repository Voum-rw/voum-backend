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
    ),

    // ── Support, Disputes & Safety ───────────────────────────────────────────
    SUPPORT_TICKET_CREATED(
            "Support Ticket Created",
            "Your support ticket has been received. Our team will review it shortly."
    ),
    SUPPORT_REPLY_RECEIVED(
            "Support Reply Received",
            "You received a reply on your support ticket."
    ),
    SUPPORT_TICKET_CLOSED(
            "Support Ticket Closed",
            "Your support ticket has been marked as closed."
    ),
    REPORT_RECEIVED(
            "Report Logged",
            "We have received a report regarding a safety or behavior concern."
    ),
    LOST_ITEM_UPDATED(
            "Lost Item Report Update",
            "There has been an update regarding your reported lost item."
    );


    private final String title;
    private final String body;

    NotificationTemplate(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
