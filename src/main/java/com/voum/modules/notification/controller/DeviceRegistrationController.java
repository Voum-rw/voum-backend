package com.voum.modules.notification.controller;

import com.voum.modules.notification.dto.DeviceRegistrationRequest;
import com.voum.modules.notification.entity.NotificationLog;
import com.voum.modules.notification.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DeviceRegistrationController {

    private final PushNotificationService pushNotificationService;

    /**
     * Register a device token for the authenticated user.
     * Idempotent — registering the same token again refreshes it.
     * <p>
     * POST /api/v1/devices/register
     */
    @PostMapping("/devices/register")
    public ResponseEntity<Map<String, String>> registerDevice(
            Authentication authentication,
            @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        pushNotificationService.registerDevice(userId, request);
        return ResponseEntity.ok(Map.of("message", "Device registered successfully."));
    }

    /**
     * Unregister (deactivate) a device token for the authenticated user.
     * <p>
     * POST /api/v1/devices/unregister
     */
    @PostMapping("/devices/unregister")
    public ResponseEntity<Map<String, String>> unregisterDevice(
            Authentication authentication,
            @RequestBody Map<String, String> body
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        String deviceToken = body.get("deviceToken");
        if (deviceToken == null || deviceToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "deviceToken must not be blank."));
        }
        pushNotificationService.unregisterDevice(userId, deviceToken);
        return ResponseEntity.ok(Map.of("message", "Device unregistered successfully."));
    }

    /**
     * Retrieve the last 20 notification log entries for the authenticated user.
     * Serves as the in-app notification centre.
     * <p>
     * GET /api/v1/notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationLog>> getNotificationHistory(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<NotificationLog> history = pushNotificationService.getNotificationHistory(userId);
        return ResponseEntity.ok(history);
    }
}
