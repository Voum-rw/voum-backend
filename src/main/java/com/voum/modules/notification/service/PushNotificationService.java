package com.voum.modules.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.modules.notification.dto.DeviceRegistrationRequest;
import com.voum.modules.notification.entity.DeviceToken;
import com.voum.modules.notification.entity.DeviceToken.Platform;
import com.voum.modules.notification.entity.NotificationLog;
import com.voum.modules.notification.entity.NotificationLog.NotificationStatus;
import com.voum.modules.notification.provider.NotificationProvider;
import com.voum.modules.notification.repository.DeviceTokenRepository;
import com.voum.modules.notification.repository.NotificationLogRepository;
import com.voum.modules.notification.templates.NotificationTemplate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Core push notification service.
 * <p>
 * Responsibilities:
 * - Register / unregister device tokens per user
 * - Dispatch push notifications to all active tokens for a user
 * - Log every attempt with full status tracking
 * - Handle invalid tokens by auto-deactivating them
 * - Retry transient failures with exponential backoff (30s, 60s, 120s)
 * - Expose Actuator counters for observability
 */
@Service
@Slf4j
public class PushNotificationService {

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_SECONDS = {30, 60, 120};
    private static final int NOTIFICATION_HISTORY_PAGE_SIZE = 20;

    private final NotificationProvider notificationProvider;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Counter invalidCounter;

    private final ScheduledExecutorService retryScheduler =
            new ScheduledThreadPoolExecutor(2, r -> {
                Thread t = new Thread(r, "notification-retry");
                t.setDaemon(true);
                return t;
            });

    public PushNotificationService(
            NotificationProvider notificationProvider,
            DeviceTokenRepository deviceTokenRepository,
            NotificationLogRepository notificationLogRepository,
            @Autowired(required = false) MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        this.notificationProvider = notificationProvider;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.objectMapper = objectMapper;

        MeterRegistry registry = meterRegistry != null
                ? meterRegistry
                : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

        this.sentCounter    = Counter.builder("voum.notifications.sent").register(registry);
        this.failedCounter  = Counter.builder("voum.notifications.failed").register(registry);
        this.invalidCounter = Counter.builder("voum.notifications.invalid").register(registry);
    }

    // ───────────────────────── Device Registration ───────────────────────────

    @Transactional
    public void registerDevice(UUID userId, DeviceRegistrationRequest request) {
        Optional<DeviceToken> existing =
                deviceTokenRepository.findByUserIdAndDeviceToken(userId, request.getDeviceToken());

        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setActive(true);
            token.setPlatform(request.getPlatform());
            token.setAppVersion(request.getAppVersion());
            deviceTokenRepository.save(token);
            log.info("Device token refreshed for user [{}]", userId);
        } else {
            DeviceToken token = DeviceToken.builder()
                    .userId(userId)
                    .deviceToken(request.getDeviceToken())
                    .platform(request.getPlatform())
                    .appVersion(request.getAppVersion())
                    .active(true)
                    .build();
            deviceTokenRepository.save(token);
            log.info("New device token registered for user [{}] on platform [{}]", userId, request.getPlatform());
        }
    }

    @Transactional
    public void unregisterDevice(UUID userId, String deviceToken) {
        deviceTokenRepository.findByUserIdAndDeviceToken(userId, deviceToken)
                .ifPresent(token -> {
                    token.setActive(false);
                    deviceTokenRepository.save(token);
                    log.info("Device token unregistered for user [{}]", userId);
                });
    }

    // ──────────────────────── Notification Dispatch ──────────────────────────

    /**
     * Sends a push notification to all active device tokens for a user.
     * Runs asynchronously so callers are never blocked.
     *
     * @param userId      Target user
     * @param template    Notification template (title + body)
     * @param dataPayload Extra data for deep linking (e.g. tripId, requestId)
     */
    @Async
    public void sendPush(UUID userId, NotificationTemplate template, Map<String, String> dataPayload) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens for user [{}] – skipping push for [{}]", userId, template);
            return;
        }

        // Enrich data with notification type for deep linking
        Map<String, String> enrichedData = new java.util.HashMap<>(dataPayload);
        enrichedData.put("type", template.name());

        for (DeviceToken token : tokens) {
            NotificationLog log_ = createPendingLog(userId, template, enrichedData);
            attemptSend(token, template, enrichedData, log_, 0);
        }
    }

    // ─────────────────────────── Internal Helpers ────────────────────────────

    private void attemptSend(DeviceToken token,
                              NotificationTemplate template,
                              Map<String, String> data,
                              NotificationLog logEntry,
                              int attempt) {
        try {
            logEntry.setAttemptCount(attempt + 1);
            boolean valid = notificationProvider.send(token.getDeviceToken(), template.getTitle(), template.getBody(), data);

            if (!valid) {
                // Token is permanently invalid
                markInvalid(token, logEntry);
            } else {
                markSent(logEntry);
            }
        } catch (Exception e) {
            log.warn("Push notification attempt {} failed for user [{}]: {}", attempt + 1, logEntry.getUserId(), e.getMessage());

            if (attempt < MAX_RETRIES - 1) {
                long delaySeconds = RETRY_DELAYS_SECONDS[attempt];
                log.info("Scheduling retry in {}s for notification [{}]", delaySeconds, logEntry.getId());
                retryScheduler.schedule(
                        () -> attemptSend(token, template, data, logEntry, attempt + 1),
                        delaySeconds,
                        TimeUnit.SECONDS
                );
            } else {
                markFailed(logEntry);
            }
        }
    }

    private NotificationLog createPendingLog(UUID userId, NotificationTemplate template, Map<String, String> data) {
        String dataJson = null;
        try {
            dataJson = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize notification data payload: {}", e.getMessage());
        }

        NotificationLog entry = NotificationLog.builder()
                .userId(userId)
                .notificationType(template.name())
                .title(template.getTitle())
                .body(template.getBody())
                .dataPayload(dataJson)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .build();

        return notificationLogRepository.save(entry);
    }

    private void markSent(NotificationLog entry) {
        entry.setStatus(NotificationStatus.SENT);
        entry.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(entry);
        sentCounter.increment();
        log.debug("Push notification [{}] marked SENT", entry.getId());
    }

    private void markFailed(NotificationLog entry) {
        entry.setStatus(NotificationStatus.FAILED);
        notificationLogRepository.save(entry);
        failedCounter.increment();
        log.error("Push notification [{}] permanently FAILED after {} attempts", entry.getId(), entry.getAttemptCount());
    }

    private void markInvalid(DeviceToken token, NotificationLog entry) {
        // Deactivate the token so we don't waste future attempts
        deviceTokenRepository.deactivateByToken(token.getDeviceToken());
        entry.setStatus(NotificationStatus.INVALID_TOKEN);
        notificationLogRepository.save(entry);
        invalidCounter.increment();
        log.warn("Device token [{}] is invalid. Token deactivated and notification marked INVALID_TOKEN.", token.getDeviceToken());
    }

    // ─────────────────────── Notification History ────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationHistory(UUID userId) {
        return notificationLogRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, NOTIFICATION_HISTORY_PAGE_SIZE)
        );
    }
}
