package com.voum.modules.onboarding.dto;

import lombok.Data;

@Data
public class AdminDocumentReviewRequest {
    private String action; // APPROVE, REQUEST_CORRECTION, REJECT
    private String rejectionReason; // IMAGE_BLURRY, DOCUMENT_EXPIRED, UNREADABLE, NAME_MISMATCH, PLATE_MISMATCH, OTHER
    private String adminNotes;
}
