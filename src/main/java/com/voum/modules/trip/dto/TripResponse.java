package com.voum.modules.trip.dto;

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
public class TripResponse {
    private UUID id;
    private Long tripNumber;
    private UUID rideRequestId;
    private UUID rideOfferId;
    private UUID passengerId;
    private UUID motariId;
    
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationAddress;
    
    private Double agreedPrice;
    private Integer estimatedArrivalMinutes;
    private Double estimatedDistanceKm;
    
    private String status;
    private String cancellationReason;
    private UUID cancelledBy;
    
    private Double currentLatitude;
    private Double currentLongitude;
    private Instant lastLocationUpdateAt;
    
    private Instant lastStatusChangeAt;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant updatedAt;
}
