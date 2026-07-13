package com.voum.modules.users;

import com.voum.common.ApiResponse;
import com.voum.modules.users.dto.UserProfileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    /** Get the authenticated user's full profile. */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(
            @AuthenticationPrincipal java.util.UUID userId) {
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved."));
    }

    /** Register or update the device's FCM push notification token. */
    @PostMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> saveDeviceToken(
            @Valid @RequestBody DeviceTokenRequest req,
            @AuthenticationPrincipal java.util.UUID userId) {
        userService.saveDeviceToken(userId, req.getFcmToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Device token saved."));
    }

    /** Update preferred language (en | fr | rw). */
    @PatchMapping("/language")
    public ResponseEntity<ApiResponse<Void>> updateLanguage(
            @Valid @RequestBody LanguageRequest req,
            @AuthenticationPrincipal java.util.UUID userId) {
        userService.updateLanguage(userId, req.getLanguage());
        return ResponseEntity.ok(ApiResponse.success(null, "Language updated."));
    }

    // ── Inline request DTOs ──────────────────────────────────────────────────

    @Data
    public static class DeviceTokenRequest {
        @NotBlank(message = "FCM token is required")
        private String fcmToken;
    }

    @Data
    public static class LanguageRequest {
        @NotBlank
        @Pattern(regexp = "en|fr|rw", message = "Language must be en, fr, or rw")
        private String language;
    }
}
