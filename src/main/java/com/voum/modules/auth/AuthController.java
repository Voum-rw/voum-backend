package com.voum.modules.auth;

import com.voum.common.ApiResponse;
import com.voum.modules.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        authService.sendOtp(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent successfully."));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        Optional<TokenResponse> tokenResponse = authService.login(req);
        if (tokenResponse.isEmpty()) {
            // OTP is correct but user is not registered yet. Return a 200 with instructions.
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("OTP verified. Registration required to complete profile.")
                    .data(null)
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success(tokenResponse.get(), "Login successful."));
    }

    @PostMapping("/register/passenger")
    public ResponseEntity<ApiResponse<TokenResponse>> registerPassenger(
            @Valid @RequestBody RegisterPassengerRequest req) {
        TokenResponse response = authService.registerPassenger(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Passenger registered successfully."));
    }

    @PostMapping("/register/motari")
    public ResponseEntity<ApiResponse<TokenResponse>> registerMotari(
            @Valid @RequestBody RegisterMotariRequest req) {
        TokenResponse response = authService.registerMotari(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Motari registered successfully. Verification pending."));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest req) {
        TokenResponse response = authService.rotateTokens(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully."));
    }
}
