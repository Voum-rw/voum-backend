package com.voum.modules.onboarding;

import com.voum.common.ApiResponse;
import com.voum.modules.onboarding.dto.AdminVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final OnboardingService onboardingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VerificationRequest>>> getPendingVerifications() {
        List<VerificationRequest> requests = onboardingService.getPendingVerifications();
        return ResponseEntity.ok(ApiResponse.success(requests, "Pending verifications retrieved successfully."));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveVerification(
            @PathVariable("id") UUID requestId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody AdminVerifyRequest req) {
        onboardingService.approveVerification(requestId, adminId, req);
        return ResponseEntity.ok(ApiResponse.success(null, "Motari profile approved successfully."));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectVerification(
            @PathVariable("id") UUID requestId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody AdminVerifyRequest req) {
        onboardingService.rejectVerification(requestId, adminId, req);
        return ResponseEntity.ok(ApiResponse.success(null, "Motari profile verification rejected."));
    }
}
