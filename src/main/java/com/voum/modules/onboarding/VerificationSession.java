package com.voum.modules.onboarding;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Motari ID is required")
    @Column(name = "motari_id", nullable = false)
    private UUID motariId;

    @Builder.Default
    @Column(nullable = false)
    private String status = "NOT_STARTED"; // NOT_STARTED, IN_PROGRESS, SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED, RESUBMISSION_REQUIRED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
