package com.voum.modules.trip.mapper;

import com.voum.modules.trip.dto.TripResponse;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.users.Motari;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    private final UserRepository userRepository;
    private final MotariRepository motariRepository;

    public TripMapper() {
        this.userRepository = null;
        this.motariRepository = null;
    }

    @Autowired
    public TripMapper(UserRepository userRepository, MotariRepository motariRepository) {
        this.userRepository = userRepository;
        this.motariRepository = motariRepository;
    }

    public TripResponse toResponse(Trip trip) {
        if (trip == null) {
            return null;
        }

        String passengerPhone = null;
        if (userRepository != null && trip.getPassengerId() != null) {
            passengerPhone = userRepository.findById(trip.getPassengerId())
                    .map(User::getPhone)
                    .orElse(null);
        }

        String motariPhone = null;
        String plateNumber = null;
        if (trip.getMotariId() != null) {
            if (userRepository != null) {
                motariPhone = userRepository.findById(trip.getMotariId())
                        .map(User::getPhone)
                        .orElse(null);
            }
            if (motariRepository != null) {
                plateNumber = motariRepository.findById(trip.getMotariId())
                        .map(Motari::getMotoPlateNumber)
                        .orElse(null);
            }
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
                .lastLocationUpdateAt(trip.getLastLocationUpdateAt())
                .lastStatusChangeAt(trip.getLastStatusChangeAt())
                .createdAt(trip.getCreatedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .updatedAt(trip.getUpdatedAt())
                .passengerPhone(passengerPhone)
                .motariPhone(motariPhone)
                .plateNumber(plateNumber)
                .build();
    }
}
