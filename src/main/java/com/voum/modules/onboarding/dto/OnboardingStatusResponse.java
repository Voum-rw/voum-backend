package com.voum.modules.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusResponse {
    private boolean completed;
    private String verificationStatus; // N/A, PENDING, UNDER_REVIEW, APPROVED, REJECTED
    private int profileCompletion; // 0 to 100
    private List<String> missingFields;
}
