package com.voum.modules.review.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripReview {

    @Id
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(name = "reviewed_user_id", nullable = false)
    private UUID reviewedUserId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Builder.Default
    @Column(name = "is_flagged", nullable = false)
    private boolean isFlagged = false;

    @Column(name = "flag_reason")
    private String flagReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "review_version", nullable = false)
    private Integer reviewVersion = 1;

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
