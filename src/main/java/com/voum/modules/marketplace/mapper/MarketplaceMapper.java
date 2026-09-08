package com.voum.modules.marketplace.mapper;

import com.voum.modules.marketplace.dto.RideOfferResponse;
import com.voum.modules.marketplace.dto.RideRequestResponse;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.users.Motari;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceMapper {

    private final UserRepository userRepository;

    public MarketplaceMapper() {
        this.userRepository = null;
    }

    @Autowired
    public MarketplaceMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public RideRequestResponse toRequestResponse(RideRequest request) {
        if (request == null) {
            return null;
        }

        String passengerPhone = null;
        if (userRepository != null && request.getPassengerId() != null) {
            passengerPhone = userRepository.findById(request.getPassengerId())
                    .map(User::getPhone)
                    .orElse(null);
        }

        return RideRequestResponse.builder()
                .id(request.getId())
                .passengerId(request.getPassengerId())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .pickupAddress(request.getPickupAddress())
                .destinationAddress(request.getDestinationAddress())
                .proposedBudget(request.getProposedBudget())
                .status(request.getStatus())
                .expiresAt(request.getExpiresAt())
                .selectedOfferId(request.getSelectedOfferId())
                .offersCount(request.getOffersCount())
                .requestVersion(request.getRequestVersion())
                .visibilityRadiusKm(request.getVisibilityRadiusKm())
                .createdArea(request.getCreatedArea())
                .createdAt(request.getCreatedAt())
                .passengerPhone(passengerPhone)
                .build();
    }

    public RideOfferResponse toOfferResponse(RideOffer offer, Motari motari, double distanceKm) {
        if (offer == null) {
            return null;
        }

        String firstName = motari != null ? motari.getFirstName() : "Unknown";
        String profileImage = motari != null ? motari.getProfileImage() : null;

        return RideOfferResponse.builder()
                .id(offer.getId())
                .rideRequestId(offer.getRideRequestId())
                .motariId(offer.getMotariId())
                .firstName(firstName)
                .profileImage(profileImage)
                .offeredPrice(offer.getOfferedPrice())
                .estimatedArrivalMinutes(offer.getEstimatedArrivalMinutes())
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0) // Round to 2 decimal places
                .status(offer.getStatus())
                .updateCount(offer.getUpdateCount())
                .trustScore(motari != null && motari.getTrustScore() != null ? motari.getTrustScore() : 50.00)
                .build();

    }
}
