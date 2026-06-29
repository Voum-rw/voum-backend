package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import com.voum.common.ApiResponse;
import com.voum.modules.onboarding.dto.OnboardingStatusResponse;
import com.voum.modules.onboarding.dto.PassengerCompleteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getStatus(
            @AuthenticationPrincipal UUID userId) {
        OnboardingStatusResponse response = onboardingService.getOnboardingStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding status retrieved successfully."));
    }

    @PostMapping("/passenger/complete")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> completePassenger(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PassengerCompleteRequest req) {
        OnboardingStatusResponse response = onboardingService.completePassengerOnboarding(userId, req);
        return ResponseEntity.ok(ApiResponse.success(response, "Passenger onboarding completed successfully."));
    }

    @PostMapping(value = "/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedDocument>> uploadDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("docType") String docType,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            throw new ApiException("Uploaded file cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] bytes = file.getBytes();
            UploadedDocument doc = onboardingService.uploadDocument(
                    userId,
                    docType.toUpperCase(),
                    bytes,
                    file.getOriginalFilename(),
                    file.getContentType()
            );
            return ResponseEntity.ok(ApiResponse.success(doc, "Document uploaded successfully."));
        } catch (IOException e) {
            throw new ApiException("Failed to read uploaded file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/motari/submit")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> submitMotari(
            @AuthenticationPrincipal UUID userId) {
        OnboardingStatusResponse response = onboardingService.submitMotariOnboarding(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Motari onboarding submitted for admin review successfully."));
    }
}
