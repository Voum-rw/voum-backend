package com.voum.modules.onboarding;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "uploaded_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @NotNull(message = "Owner ID is required")
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "category")
    private String category; // PERSONAL, MOTORCYCLE, DRIVING

    @NotBlank(message = "Document type is required")
    @Column(name = "document_type", nullable = false)
    private String documentType; // NATIONAL_ID_FRONT, NATIONAL_ID_BACK, PROFILE_IMAGE, DRIVING_PERMIT_FRONT, DRIVING_PERMIT_BACK, VEHICLE_REGISTRATION, INSURANCE_CERTIFICATE, PLATE_PHOTO

    @Column(name = "side")
    private String side; // FRONT, BACK, SINGLE

    @NotBlank(message = "File URL is required")
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "storage_key")
    private String storageKey; // Object key in R2 or local storage

    @NotBlank(message = "File name is required")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @NotNull(message = "File size is required")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_checksum")
    private String fileChecksum;

    @Builder.Default
    @Column(name = "version")
    private Integer version = 1;

    @Builder.Default
    @Column(name = "status")
    private String status = "UPLOADED"; // UPLOADED, UNDER_REVIEW, APPROVED, NEEDS_CORRECTION, REJECTED

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "extracted_plate_number")
    private String extractedPlateNumber;

    @Column(name = "extracted_permit_number")
    private String extractedPermitNumber;

    @Column(name = "extracted_national_id")
    private String extractedNationalId;

    @Column(name = "ocr_metadata_json", columnDefinition = "TEXT")
    private String ocrMetadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
