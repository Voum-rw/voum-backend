package com.voum.modules.marketplace.dto;

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
public class RideRequestResponse {
    private UUID id;
    private UUID passengerId;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String pickupAddress;
    private String destinationAddress;
    private Double proposedBudget;
    private String status;
    private Instant expiresAt;
    private UUID selectedOfferId;
    private Integer offersCount;
    private Integer requestVersion;
    private Double visibilityRadiusKm;
    private String createdArea;
    private Instant createdAt;
}
