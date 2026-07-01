package com.voum.modules.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostItem {

    public enum LostItemStatus {
        REPORTED,
        FOUND,
        RETURNED,
        RESOLVED
    }

    @Id
    private UUID id;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "reported_by")
    private UUID reportedBy;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LostItemStatus status;

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
