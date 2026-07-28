package com.voum.modules.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "motaris")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motari implements Persistable<UUID> {

    @Id
    private UUID id;

    /**
     * Transient flag used by Spring Data JPA to determine whether to call
     * persist() or merge() on save(). This is required because @MapsId sets
     * the ID explicitly, which would otherwise cause JPA to always call
     * merge() (UPDATE) instead of persist() (INSERT) for new entities.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotBlank(message = "National ID is required")
    @Column(name = "national_id", nullable = false, unique = true)
    private String nationalId;

    @NotBlank(message = "Moto plate number is required")
    @Column(name = "moto_plate_number", nullable = false, unique = true)
    private String motoPlateNumber;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "moto_model")
    private String motoModel;

    @Column(name = "moto_color")
    private String motoColor;

    @Builder.Default
    @Column(name = "verification_status", nullable = false)
    private String verificationStatus = "PENDING"; // PENDING, UNDER_REVIEW, APPROVED, REJECTED, SUSPENDED

    @Builder.Default
    @Column(name = "onboarding_status", nullable = false)
    private String onboardingStatus = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED

    @Column(name = "verification_request_id")
    private UUID verificationRequestId;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED

    @Column(name = "average_rating")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.DECIMAL)
    private Double averageRating;

    @Builder.Default
    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews = 0;

    @Builder.Default
    @Column(name = "total_completed_trips", nullable = false)
    private Integer totalCompletedTrips = 0;

    @Builder.Default
    @Column(name = "completion_rate", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.DECIMAL)
    private Double completionRate = 100.00;

    @Builder.Default
    @Column(name = "acceptance_rate", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.DECIMAL)
    private Double acceptanceRate = 100.00;

    @Builder.Default
    @Column(name = "cancellation_rate", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.DECIMAL)
    private Double cancellationRate = 0.00;

    @Builder.Default
    @Column(name = "trust_score", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.DECIMAL)
    private Double trustScore = 50.00;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
