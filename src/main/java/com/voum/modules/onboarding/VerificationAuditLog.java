package com.voum.modules.onboarding;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "document_id")
    private UUID documentId;

    @NotBlank(message = "Action is required")
    @Column(name = "action", nullable = false)
    private String action; // DOCUMENT_UPLOADED, DOCUMENT_VIEWED, DOCUMENT_APPROVED, CORRECTION_REQUESTED, DOCUMENT_REJECTED, STATUS_CHANGED

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
}
