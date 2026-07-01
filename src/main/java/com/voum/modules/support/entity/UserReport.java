package com.voum.modules.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReport {

    public enum ReportSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ReportStatus {
        PENDING,
        INVESTIGATING,
        RESOLVED
    }

    @Id
    private UUID id;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "reported_user_id", nullable = false)
    private UUID reportedUserId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private ReportSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status;

    @Column(name = "resolution_summary", length = 4000)
    private String resolutionSummary;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
