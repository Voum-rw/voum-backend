package com.voum.modules.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLocationUpdatedMessage {
    @Builder.Default
    private String eventType = "LOCATION_UPDATED";
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Instant recordedAt;
}
