package com.voum.modules.admin.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripAdminResponse {
    private UUID id;
    private Long tripNumber;
    private UUID rideRequestId;
    private UUID rideOfferId;
    private String status;
    private Double agreedPrice;
    private Integer estimatedArrivalMinutes;
    private Double estimatedDistanceKm;
    private String pickupAddress;
    private String destinationAddress;
    private String cancellationReason;
    private UUID cancelledBy;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    // Timeline milestones
    private Map<String, Instant> timeline;

    // Participant Details
    private ParticipantDetails passenger;
    private ParticipantDetails motari;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDetails {
        private UUID id;
        private String name;
        private String phone;
        private Double rating;
        private String motoPlateNumber; // Motari only
    }
}
