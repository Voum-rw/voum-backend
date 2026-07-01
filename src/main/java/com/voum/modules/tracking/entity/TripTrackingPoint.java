package com.voum.modules.tracking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_tracking_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripTrackingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @NotNull
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @NotNull
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @NotNull
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "accuracy", nullable = false)
    private Double accuracy;

    @NotNull
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "speed_kmh", nullable = false)
    private Double speedKmh;

    @NotNull
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "heading_degrees", nullable = false)
    private Double headingDegrees;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "gps_mocked")
    private Boolean gpsMocked;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
