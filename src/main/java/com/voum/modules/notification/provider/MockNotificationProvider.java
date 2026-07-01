package com.voum.modules.notification.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Development-mode NotificationProvider.
 * Logs notifications to the console. No external calls are made.
 * Registered by FirebaseConfig when Firebase credentials are not available.
 */
public class MockNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationProvider.class);

    @Override
    public boolean send(String deviceToken, String title, String body, Map<String, String> data) {
        log.info("[MOCK PUSH] Token={} | Title='{}' | Body='{}' | Data={}", deviceToken, title, body, data);
        return true;
    }
}
