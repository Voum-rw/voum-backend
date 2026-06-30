package com.voum.modules.marketplace.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "ride_request_id", nullable = false)
    private UUID rideRequestId;

    @NotNull
    @Column(name = "motari_id", nullable = false)
    private UUID motariId;

    @NotNull
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.DECIMAL)
    @Column(name = "offered_price", nullable = false)
    private Double offeredPrice;

    @NotNull
    @Column(name = "estimated_arrival_minutes", nullable = false)
    private Integer estimatedArrivalMinutes;

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED, WITHDRAWN

    @Builder.Default
    @Column(name = "update_count", nullable = false)
    private Integer updateCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
