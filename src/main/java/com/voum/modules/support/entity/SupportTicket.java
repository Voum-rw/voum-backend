package com.voum.modules.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    public enum TicketType {
        GENERAL_SUPPORT,
        PAYMENT_ISSUE,
        DRIVER_COMPLAINT,
        PASSENGER_COMPLAINT,
        LOST_ITEM,
        TECHNICAL_ISSUE,
        SAFETY_EMERGENCY,
        DISPUTE_PRICE,
        DISPUTE_CANCELLATION,
        DISPUTE_BEHAVIOR,
        DISPUTE_ROUTE,
        DISPUTE_FRAUD
    }

    public enum TicketStatus {
        OPEN,
        ASSIGNED,
        RESOLVED,
        REOPENED,
        CLOSED
    }

    public enum TicketPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TicketType type;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TicketPriority priority;

    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;

    @Column(name = "internal_notes", length = 4000)
    private String internalNotes;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_summary", length = 4000)
    private String resolutionSummary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
