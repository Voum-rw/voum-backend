package com.voum.modules.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationMeResponse {
    private UUID sessionId;
    private String status; // NOT_STARTED, IN_PROGRESS, SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED, RESUBMISSION_REQUIRED
    private String verificationLevel; // LEVEL_0, LEVEL_1, LEVEL_2, LEVEL_3
    private int progress; // 0 to 100
    private boolean canGoOnline;
    private String rejectionReason;
    private String plateNumber;
    private String nationalIdNumber;
    private String permitNumber;
    private LocalDate permitExpiryDate;
    private LocalDate insuranceExpiryDate;

    private List<CategoryStatus> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatus {
        private String category; // PERSONAL, MOTORCYCLE, DRIVING
        private String title;
        private boolean completed;
        private List<DocumentItemStatus> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentItemStatus {
        private String documentType;
        private String title;
        private boolean required;
        private SideStatus front;
        private SideStatus back;
        private SideStatus single;
        private String itemStatus; // NOT_STARTED, UPLOADED, UNDER_REVIEW, APPROVED, NEEDS_CORRECTION, REJECTED
        private String rejectionReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SideStatus {
        private boolean uploaded;
        private String documentId;
        private String status; // UPLOADED, UNDER_REVIEW, APPROVED, NEEDS_CORRECTION, REJECTED
        private String rejectionReason;
        private LocalDate expiryDate;
    }
}
