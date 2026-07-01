package com.voum.modules.notification.provider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Configures Firebase Admin SDK.
 * <p>
 * Credential resolution order:
 * 1. FIREBASE_CREDENTIALS environment variable (JSON string) — for production / Render.
 * 2. firebase-service-account.json on the classpath — for local development.
 * 3. If neither is found, application boots in MockProvider mode — no startup crash.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public NotificationProvider notificationProvider() {
        try {
            InputStream credentialStream = resolveCredentials();
            if (credentialStream == null) {
                log.warn("[Firebase] No credentials found. Booting in MOCK NOTIFICATION mode. "
                        + "Set FIREBASE_CREDENTIALS env var or place firebase-service-account.json on classpath.");
                return new MockNotificationProvider();
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialStream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("[Firebase] FirebaseApp initialized successfully.");
            }

            // Register FirebaseApp as a named bean for ConditionalOnBean to detect
            return new FirebaseNotificationProvider();
        } catch (Exception e) {
            log.warn("[Firebase] Failed to initialise Firebase ({}). Booting in MOCK NOTIFICATION mode.", e.getMessage());
            return new MockNotificationProvider();
        }
    }

    private InputStream resolveCredentials() {
        // Priority 1: Environment variable
        String credJson = System.getenv("FIREBASE_CREDENTIALS");
        if (credJson != null && !credJson.isBlank()) {
            log.info("[Firebase] Loading credentials from FIREBASE_CREDENTIALS environment variable.");
            return new ByteArrayInputStream(credJson.getBytes(StandardCharsets.UTF_8));
        }

        // Priority 2: Classpath file
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (resource.exists()) {
                log.info("[Firebase] Loading credentials from classpath:firebase-service-account.json.");
                return resource.getInputStream();
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
