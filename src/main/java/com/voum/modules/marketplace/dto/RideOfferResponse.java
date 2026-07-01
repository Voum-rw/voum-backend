package com.voum.modules.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferResponse {
    private UUID id;
    private UUID rideRequestId;
    private UUID motariId;
    private String firstName;
    private String profileImage;
    private Double offeredPrice;
    private Integer estimatedArrivalMinutes;
    private Double distanceKm;
    private String status;
    private Integer updateCount;
    private Double trustScore;
}

