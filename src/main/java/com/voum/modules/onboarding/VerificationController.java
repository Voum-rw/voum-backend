package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import com.voum.common.ApiResponse;
import com.voum.modules.onboarding.dto.VerificationMeResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final OnboardingService onboardingService;

    /** Single source of truth API for Motari verification state. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<VerificationMeResponse>> getMeVerification(
            @AuthenticationPrincipal UUID userId) {
        VerificationMeResponse response = onboardingService.getVerificationMeStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Verification profile retrieved successfully."));
    }

    /** Upload verification document item with category, side, and optional expiry date. */
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedDocument>> uploadVmsDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("docType") String docType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "side", required = false) String side,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ApiException("Uploaded file cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] bytes = file.getBytes();
            UploadedDocument doc = onboardingService.uploadVmsDocument(
                    userId,
                    docType.toUpperCase(),
                    category,
                    side,
                    bytes,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    expiryDate
            );
            return ResponseEntity.ok(ApiResponse.success(doc, "Document uploaded successfully."));
        } catch (IOException e) {
            throw new ApiException("Failed to read uploaded file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Save structured driver profile metadata (plate number, permit number, etc.). */
    @PostMapping("/profile-data")
    public ResponseEntity<ApiResponse<MotariVerificationProfile>> saveProfileData(
            @AuthenticationPrincipal UUID userId,
            @RequestBody ProfileDataRequest req) {
        MotariVerificationProfile profile = onboardingService.saveMotariProfileData(
                userId, req.getPlateNumber(), req.getNationalIdNumber(), req.getPermitNumber(), req.getPermitExpiryDate(), req.getInsuranceExpiryDate());
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile verification metadata updated successfully."));
    }

    /** Submit verification session for admin review. */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<VerificationMeResponse>> submitVerification(
            @AuthenticationPrincipal UUID userId) {
        onboardingService.submitMotariOnboarding(userId);
        VerificationMeResponse response = onboardingService.getVerificationMeStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Verification session submitted for review successfully."));
    }

    @Data
    public static class ProfileDataRequest {
        private String plateNumber;
        private String nationalIdNumber;
        private String permitNumber;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate permitExpiryDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate insuranceExpiryDate;
    }
}
