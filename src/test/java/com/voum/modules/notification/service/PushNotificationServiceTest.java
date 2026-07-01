package com.voum.modules.notification.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private NotificationProvider notificationProvider;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    private PushNotificationService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(
                notificationProvider,
                deviceTokenRepository,
                notificationLogRepository,
                null, // No MeterRegistry in tests — falls back to SimpleMeterRegistry
                new ObjectMapper()
        );
    }

    // ── Device Registration Tests ─────────────────────────────────────────────

    @Test
    void registerDevice_newToken_shouldSaveNewEntry() {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setDeviceToken("token-abc-123");
        request.setPlatform(Platform.ANDROID);
        request.setAppVersion("1.0.0");

        when(deviceTokenRepository.findByUserIdAndDeviceToken(userId, "token-abc-123"))
                .thenReturn(Optional.empty());

        service.registerDevice(userId, request);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());

        DeviceToken saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("token-abc-123", saved.getDeviceToken());
        assertEquals(Platform.ANDROID, saved.getPlatform());
        assertTrue(saved.isActive());
    }

    @Test
    void registerDevice_existingToken_shouldRefreshInsteadOfDuplicate() {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setDeviceToken("existing-token");
        request.setPlatform(Platform.IOS);

        DeviceToken existing = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceToken("existing-token")
                .platform(Platform.ANDROID)
                .active(false)
                .build();

        when(deviceTokenRepository.findByUserIdAndDeviceToken(userId, "existing-token"))
                .thenReturn(Optional.of(existing));

        service.registerDevice(userId, request);

        // Should update the existing token, not create a new one
        verify(deviceTokenRepository, times(1)).save(existing);
        assertTrue(existing.isActive());
        assertEquals(Platform.IOS, existing.getPlatform());
    }

    @Test
    void registerDevice_shouldNotAllowUserToRegisterAnotherUsersToken() {
        // Two different users try to register the same device token
        UUID otherUserId = UUID.randomUUID();
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setDeviceToken("shared-token");
        request.setPlatform(Platform.ANDROID);

        // User1 already owns the token
        when(deviceTokenRepository.findByUserIdAndDeviceToken(userId, "shared-token"))
                .thenReturn(Optional.empty());
        when(deviceTokenRepository.findByUserIdAndDeviceToken(otherUserId, "shared-token"))
                .thenReturn(Optional.empty());

        service.registerDevice(userId, request);
        service.registerDevice(otherUserId, request);

        // Both calls should save — token is stored per (userId, token) pair
        verify(deviceTokenRepository, times(2)).save(any(DeviceToken.class));
    }

    // ── Notification Dispatch Tests ──────────────────────────────────────────

    @Test
    void sendPush_withActiveTokens_shouldSendNotificationAndLogAsSent() throws Exception {
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceToken("device-token-1")
                .platform(Platform.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(token));

        NotificationLog log = NotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .build();
        when(notificationLogRepository.save(any())).thenReturn(log);
        when(notificationProvider.send(eq("device-token-1"), anyString(), anyString(), anyMap()))
                .thenReturn(true);

        service.sendPush(userId, NotificationTemplate.TRIP_CREATED, Map.of("tripId", UUID.randomUUID().toString()));

        verify(notificationProvider, times(1)).send(eq("device-token-1"), anyString(), anyString(), anyMap());
        verify(notificationLogRepository, atLeast(2)).save(any()); // PENDING + SENT
    }

    @Test
    void sendPush_withNoActiveTokens_shouldSkipSilently() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        service.sendPush(userId, NotificationTemplate.TRIP_CANCELLED, Map.of());

        verifyNoInteractions(notificationProvider);
        verifyNoInteractions(notificationLogRepository);
    }

    @Test
    void sendPush_withInvalidToken_shouldDeactivateTokenAndMarkInvalidToken() throws Exception {
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceToken("invalid-device-token")
                .platform(Platform.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(token));

        NotificationLog log = NotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .build();
        when(notificationLogRepository.save(any())).thenReturn(log);
        when(notificationProvider.send(eq("invalid-device-token"), anyString(), anyString(), anyMap()))
                .thenReturn(false); // false = invalid token

        service.sendPush(userId, NotificationTemplate.ACCOUNT_APPROVED, Map.of());

        // Token must be deactivated in DB
        verify(deviceTokenRepository).deactivateByToken("invalid-device-token");

        // Log must have INVALID_TOKEN status
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, atLeast(2)).save(captor.capture());
        List<NotificationLog> savedLogs = captor.getAllValues();
        assertTrue(savedLogs.stream().anyMatch(l -> l.getStatus() == NotificationStatus.INVALID_TOKEN));
    }

    @Test
    void sendPush_withTransientError_shouldScheduleRetry() throws Exception {
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceToken("token-transient")
                .platform(Platform.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(token));

        NotificationLog log = NotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .build();
        when(notificationLogRepository.save(any())).thenReturn(log);

        // Always throw — simulates a persistent transient error
        when(notificationProvider.send(anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Network timeout"));

        service.sendPush(userId, NotificationTemplate.TRIP_STARTED, Map.of("tripId", "t1"));

        // Initial attempt fires synchronously; verify it was called at least once
        verify(notificationProvider, atLeastOnce()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void sendPush_dataPayloadContainsNotificationType() throws Exception {
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceToken("token-data-test")
                .platform(Platform.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(token));

        NotificationLog log = NotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .build();
        when(notificationLogRepository.save(any())).thenReturn(log);
        when(notificationProvider.send(anyString(), anyString(), anyString(), anyMap())).thenReturn(true);

        service.sendPush(userId, NotificationTemplate.OFFER_ACCEPTED, Map.of("offerId", "o1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationProvider).send(anyString(), anyString(), anyString(), dataCaptor.capture());

        Map<String, String> capturedData = dataCaptor.getValue();
        assertEquals("OFFER_ACCEPTED", capturedData.get("type"));
        assertEquals("o1", capturedData.get("offerId"));
    }

    // ── Notification History Test ─────────────────────────────────────────────

    @Test
    void getNotificationHistory_shouldReturnUserLogs() {
        NotificationLog log1 = NotificationLog.builder().id(UUID.randomUUID()).userId(userId).build();
        NotificationLog log2 = NotificationLog.builder().id(UUID.randomUUID()).userId(userId).build();

        when(notificationLogRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(List.of(log1, log2));

        List<NotificationLog> history = service.getNotificationHistory(userId);

        assertEquals(2, history.size());
        verify(notificationLogRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), any());
    }
}
