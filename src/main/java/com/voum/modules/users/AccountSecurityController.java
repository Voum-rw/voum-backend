package com.voum.modules.users;

import com.voum.common.ApiException;
import com.voum.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/security")
@RequiredArgsConstructor
public class AccountSecurityController {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;

    @PostMapping("/password")
    @Transactional
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UUID id, @Valid @RequestBody PasswordChange request) {
        User user = authenticated(id, request.getCurrentPassword());
        user.setPassword(passwords.encode(request.getNewPassword()));
        users.save(user);
        refreshTokens.deleteByUser(user);
        return ApiResponse.success(null, "Password changed. Sign in again.");
    }

    @PostMapping("/email")
    @Transactional
    public ApiResponse<Void> enrollEmail(@AuthenticationPrincipal UUID id, @Valid @RequestBody EmailEnrollment request) {
        User user = authenticated(id, request.getCurrentPassword());
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            throw new ApiException("Email is already set and cannot be changed here.", HttpStatus.CONFLICT);
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException("Email is already registered.", HttpStatus.CONFLICT);
        user.setEmail(email);
        users.save(user);
        return ApiResponse.success(null, "Email added for sign-in.");
    }

    private User authenticated(UUID id, String password) {
        User user = users.findForAccountUpdate(id).orElseThrow(() -> new ApiException("Account not found.", HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsBlocked()) || user.getPassword() == null || !passwords.matches(password, user.getPassword())) {
            throw new ApiException("Current password is incorrect or account is unavailable.", HttpStatus.FORBIDDEN);
        }
        return user;
    }
    @Data public static class PasswordChange {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min = 8, max = 72) private String newPassword;
    }
    @Data public static class EmailEnrollment {
        @NotBlank private String currentPassword;
        @NotBlank @Email @Size(max = 255) private String email;
    }
}
