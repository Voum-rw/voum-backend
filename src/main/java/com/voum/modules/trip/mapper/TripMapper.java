package com.voum.modules.trip.mapper;

import com.voum.modules.trip.dto.TripResponse;
import com.voum.modules.trip.entity.Trip;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public TripResponse toResponse(Trip trip) {
        if (trip == null) {
            return null;
        }

        return TripResponse.builder()
                .id(trip.getId())
                .tripNumber(trip.getTripNumber())
                .rideRequestId(trip.getRideRequestId())
                .rideOfferId(trip.getRideOfferId())
                .passengerId(trip.getPassengerId())
                .motariId(trip.getMotariId())
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .pickupAddress(trip.getPickupAddress())
                .destinationLatitude(trip.getDestinationLatitude())
                .destinationLongitude(trip.getDestinationLongitude())
                .destinationAddress(trip.getDestinationAddress())
                .agreedPrice(trip.getAgreedPrice())
                .estimatedArrivalMinutes(trip.getEstimatedArrivalMinutes())
                .estimatedDistanceKm(trip.getEstimatedDistanceKm())
                .status(trip.getStatus())
                .cancellationReason(trip.getCancellationReason())
                .cancelledBy(trip.getCancelledBy())
                .currentLatitude(trip.getCurrentLatitude())
                .currentLongitude(trip.getCurrentLongitude())
                .lastStatusChangeAt(trip.getLastStatusChangeAt())
                .createdAt(trip.getCreatedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}
