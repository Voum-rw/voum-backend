package com.voum.modules.marketplace.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Passenger ID is required")
    @Column(name = "passenger_id", nullable = false)
    private UUID passengerId;

    @NotNull
    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @NotNull
    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @NotNull
    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @NotNull
    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "destination_address")
    private String destinationAddress;

    @NotNull
    @Column(name = "proposed_budget")
    private Double proposedBudget;

    @Builder.Default
    @Column(nullable = false)
    private String status = "OPEN"; // OPEN, EXPIRED, MATCHED, CANCELLED

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "selected_offer_id")
    private UUID selectedOfferId;

    @Builder.Default
    @Column(name = "offers_count", nullable = false)
    private Integer offersCount = 0;

    @Builder.Default
    @Column(name = "request_version", nullable = false)
    private Integer requestVersion = 1;

    @Builder.Default
    @Column(name = "visibility_radius_km", nullable = false)
    private Double visibilityRadiusKm = 3.00;

    @Column(name = "created_area")
    private String createdArea;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
