package com.voum.modules.trip.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_number", insertable = false, updatable = false)
    private Long tripNumber; // DB BIGSERIAL unique number

    @NotNull
    @Column(name = "ride_request_id", nullable = false)
    private UUID rideRequestId;

    @NotNull
    @Column(name = "ride_offer_id", nullable = false)
    private UUID rideOfferId;

    @NotNull
    @Column(name = "passenger_id", nullable = false)
    private UUID passengerId;

    @NotNull
    @Column(name = "motari_id", nullable = false)
    private UUID motariId;

    @NotNull
    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @NotNull
    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @NotNull
    @Column(name = "destination_latitude", nullable = false)
    private Double destinationLatitude;

    @NotNull
    @Column(name = "destination_longitude", nullable = false)
    private Double destinationLongitude;

    @Column(name = "destination_address")
    private String destinationAddress;

    @NotNull
    @Column(name = "agreed_price", nullable = false)
    private Double agreedPrice;

    @Column(name = "estimated_arrival_minutes")
    private Integer estimatedArrivalMinutes;

    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;

    @NotNull
    @Column(nullable = false)
    private String status; // CREATED, MOTARI_EN_ROUTE, MOTARI_ARRIVED, PASSENGER_ONBOARD, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @NotNull
    @Column(name = "last_status_change_at", nullable = false)
    private Instant lastStatusChangeAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
