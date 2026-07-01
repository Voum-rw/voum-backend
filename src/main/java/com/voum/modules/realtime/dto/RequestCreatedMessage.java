package com.voum.modules.realtime.dto;

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
public class RequestCreatedMessage {
    private String eventType; // "REQUEST_CREATED"
    private int eventVersion;
    private long sequence;
    
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
    private Integer offersCount;
    private Integer requestVersion;
    private Double visibilityRadiusKm;
    private String createdArea;
    private Instant createdAt;
}
