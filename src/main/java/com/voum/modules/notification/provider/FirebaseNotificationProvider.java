package com.voum.modules.notification.provider;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Firebase Cloud Messaging implementation of NotificationProvider.
 * Instantiated by FirebaseConfig when valid credentials are available.
 */
public class FirebaseNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FirebaseNotificationProvider.class);

    // FCM error codes that mean the token is permanently invalid
    private static final java.util.Set<String> INVALID_TOKEN_CODES = java.util.Set.of(
            "registration-token-not-registered",
            "invalid-registration-token",
            "invalid-argument"
    );

    @Override
    public boolean send(String deviceToken, String title, String body, Map<String, String> data) throws Exception {
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();

        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder().setSound("default").build())
                .build();

        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM message sent successfully: {}", messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            String errorCode = e.getMessagingErrorCode() != null
                    ? e.getMessagingErrorCode().name().toLowerCase().replace("_", "-")
                    : "";
            if (INVALID_TOKEN_CODES.contains(errorCode)) {
                log.warn("FCM token is invalid/expired: {}", deviceToken);
                return false; // Signal invalid token to caller
            }
            // Re-throw transient errors so the retry mechanism handles them
            throw e;
        }
    }
}
