package com.voum.modules.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLocationResponse {
    private UUID tripId;
    private Long sequenceNumber;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Double speedKmh;
    private Double headingDegrees;
    private Integer batteryLevel;
    private Boolean gpsMocked;
    private Instant recordedAt;
}
