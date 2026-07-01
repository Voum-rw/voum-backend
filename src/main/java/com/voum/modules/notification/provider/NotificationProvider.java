package com.voum.modules.notification.provider;

import java.util.Map;

/**
 * Abstraction over push notification delivery mechanisms.
 * Implementing Firebase today; extensible to SMS, Email, or WhatsApp later.
 */
public interface NotificationProvider {

    /**
     * Sends a push notification to a single device token.
     *
     * @param deviceToken The FCM registration token
     * @param title       Notification title
     * @param body        Notification body
     * @param data        Key-value data payload (e.g. type, tripId, requestId)
     * @return true if the message was sent successfully; false if the token is invalid/expired
     * @throws Exception for transient failures (network, quota) that warrant retry
     */
    boolean send(String deviceToken, String title, String body, Map<String, String> data) throws Exception;
}
