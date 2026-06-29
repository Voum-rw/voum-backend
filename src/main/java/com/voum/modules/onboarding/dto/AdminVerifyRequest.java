package com.voum.modules.onboarding.dto;

import lombok.Data;

@Data
public class AdminVerifyRequest {
    private String rejectionReason;
    private String adminNotes;
}
