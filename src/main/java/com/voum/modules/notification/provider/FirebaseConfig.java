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
import java.util.Map;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configures Firebase Admin SDK.
 * <p>
 * Credential resolution order:
 * 1. FIREBASE_CREDENTIALS environment variable (JSON string).
 * 2. Uppercase service-account fields, optionally prefixed with FIREBASE_.
 * 3. firebase-service-account.json on the classpath for local development.
 * 4. If none are found, application boots in MockProvider mode.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public NotificationProvider notificationProvider() {
        try (InputStream credentialStream = resolveCredentials()) {
            if (credentialStream == null) {
                log.warn("[Firebase] No credentials found. Booting in MOCK NOTIFICATION mode. "
                        + "Configure FIREBASE_CREDENTIALS JSON or the service-account environment fields.");
                return new MockNotificationProvider();
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialStream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("[Firebase] FirebaseApp initialized successfully.");
            }

            return new FirebaseNotificationProvider();
        } catch (Exception e) {
            // Parser exceptions may contain credential contents: never log their messages.
            log.warn("[Firebase] Failed to initialise Firebase ({}). Check credential format. Booting in MOCK NOTIFICATION mode.", e.getClass().getSimpleName());
            return new MockNotificationProvider();
        }
    }

    private InputStream resolveCredentials() throws Exception {
        // Priority 1: Environment variable
        String credJson = credentialJson(System.getenv());
        if (credJson != null && !credJson.isBlank()) {
            log.info("[Firebase] Loading service-account credentials from environment variables.");
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

    /** Accept whole JSON or uppercase JSON field names, optionally FIREBASE_-prefixed. */
    static String credentialJson(Map<String, String> environment) throws Exception {
        String raw = environment.get("FIREBASE_CREDENTIALS");
        if (raw != null && !raw.isBlank()) return raw;
        Map<String, String> fields = new LinkedHashMap<>();
        for (String key : new String[]{"type", "project_id", "private_key_id", "private_key", "client_email", "client_id", "auth_uri", "token_uri", "auth_provider_x509_cert_url", "client_x509_cert_url", "universe_domain"}) {
            String envKey = key.toUpperCase(java.util.Locale.ROOT);
            String value = environment.get("FIREBASE_" + envKey);
            if (value == null || value.isBlank()) value = environment.get(envKey);
            if (value != null && !value.isBlank()) fields.put(key, value);
        }
        // Generic environment names must not accidentally select a partial service account.
        if (!fields.containsKey("private_key") && !fields.containsKey("client_email")) return null;
        for (String required : new String[]{"project_id", "private_key", "client_email"}) {
            if (!fields.containsKey(required)) throw new IllegalArgumentException("Incomplete Firebase service-account fields");
        }
        fields.putIfAbsent("type", "service_account");
        fields.putIfAbsent("token_uri", "https://oauth2.googleapis.com/token");
        fields.put("private_key", fields.get("private_key").replace("\\n", "\n"));
        return new ObjectMapper().writeValueAsString(fields);
    }
}
