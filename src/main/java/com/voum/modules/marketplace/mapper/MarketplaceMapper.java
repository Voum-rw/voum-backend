package com.voum.modules.marketplace.mapper;

import com.voum.modules.marketplace.dto.RideOfferResponse;
import com.voum.modules.marketplace.dto.RideRequestResponse;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.users.Motari;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceMapper {

    public RideRequestResponse toRequestResponse(RideRequest request) {
        if (request == null) {
            return null;
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
                .build();
    }
}
