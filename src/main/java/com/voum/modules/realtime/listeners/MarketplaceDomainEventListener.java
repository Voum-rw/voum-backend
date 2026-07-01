package com.voum.modules.realtime.listeners;

import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.*;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.realtime.dto.*;
import com.voum.modules.realtime.publishers.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketplaceDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceDomainEventListener.class);

    private final RedisMessagePublisher redisPublisher;
    private final LocationService locationService;
    private final RideRequestRepository rideRequestRepository;

    @EventListener
    public void handleRequestCreated(RideRequestCreatedEvent event) {
        RideRequest request = event.getRideRequest();
        log.info("Handling RideRequestCreatedEvent for request: {}", request.getId());

        // Find nearby online, approved, non-busy drivers within visibility radius
        List<NearbyMotariResponse> nearby = locationService.findNearbyMotaris(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getVisibilityRadiusKm()
        );

        List<UUID> driverIds = nearby.stream()
                .map(NearbyMotariResponse::getMotariId)
                .collect(Collectors.toList());

        log.info("Found {} nearby drivers for request {}", driverIds.size(), request.getId());

        RequestCreatedMessage payload = RequestCreatedMessage.builder()
                .eventType("REQUEST_CREATED")
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
                .offersCount(request.getOffersCount())
                .requestVersion(request.getRequestVersion())
                .visibilityRadiusKm(request.getVisibilityRadiusKm())
                .createdArea(request.getCreatedArea())
                .createdAt(request.getCreatedAt())
                .build();

        // Publish to Redis channel to broadcast to the targeted nearby drivers
        redisPublisher.publish("REQUEST_CREATED", null, driverIds, payload);
    }

    @EventListener
    public void handleOfferSubmitted(RideOfferSubmittedEvent event) {
        RideOffer offer = event.getRideOffer();
        log.info("Handling RideOfferSubmittedEvent for offer: {}", offer.getId());

        RideRequest request = rideRequestRepository.findById(offer.getRideRequestId()).orElse(null);
        int offersCount = request != null ? request.getOffersCount() : 0;

        OfferCreatedMessage payload = OfferCreatedMessage.builder()
                .eventType("NEW_OFFER")
                .offerId(offer.getId())
                .requestId(offer.getRideRequestId())
                .motariId(offer.getMotariId())
                .price(offer.getOfferedPrice())
                .estimatedArrivalMinutes(offer.getEstimatedArrivalMinutes())
                .offersCount(offersCount)
                .build();

        // Publish to Redis channel to broadcast to the passenger topic
        redisPublisher.publish("NEW_OFFER", offer.getRideRequestId(), null, payload);
    }

    @EventListener
    public void handleOfferUpdated(RideOfferUpdatedEvent event) {
        RideOffer offer = event.getRideOffer();
        log.info("Handling RideOfferUpdatedEvent for offer: {}", offer.getId());

        RideRequest request = rideRequestRepository.findById(offer.getRideRequestId()).orElse(null);
        int offersCount = request != null ? request.getOffersCount() : 0;

        OfferUpdatedMessage payload = OfferUpdatedMessage.builder()
                .eventType("OFFER_UPDATED")
                .offerId(offer.getId())
                .requestId(offer.getRideRequestId())
                .motariId(offer.getMotariId())
                .price(offer.getOfferedPrice())
                .estimatedArrivalMinutes(offer.getEstimatedArrivalMinutes())
                .offersCount(offersCount)
                .build();

        redisPublisher.publish("OFFER_UPDATED", offer.getRideRequestId(), null, payload);
    }

    @EventListener
    public void handleOfferWithdrawn(RideOfferWithdrawnEvent event) {
        RideOffer offer = event.getRideOffer();
        log.info("Handling RideOfferWithdrawnEvent for offer: {}", offer.getId());

        RideRequest request = rideRequestRepository.findById(offer.getRideRequestId()).orElse(null);
        int offersCount = request != null ? request.getOffersCount() : 0;

        OfferWithdrawnMessage payload = OfferWithdrawnMessage.builder()
                .eventType("OFFER_WITHDRAWN")
                .offerId(offer.getId())
                .requestId(offer.getRideRequestId())
                .motariId(offer.getMotariId())
                .offersCount(offersCount)
                .build();

        redisPublisher.publish("OFFER_WITHDRAWN", offer.getRideRequestId(), null, payload);
    }

    @EventListener
    public void handleOfferAccepted(RideOfferAcceptedEvent event) {
        RideRequest request = event.getRideRequest();
        RideOffer offer = event.getRideOffer();
        log.info("Handling RideOfferAcceptedEvent for request: {}", request.getId());

        OfferAcceptedMessage payload = OfferAcceptedMessage.builder()
                .eventType("OFFER_ACCEPTED")
                .requestId(request.getId())
                .offerId(offer.getId())
                .motariId(offer.getMotariId())
                .build();

        redisPublisher.publish("OFFER_ACCEPTED", request.getId(), null, payload);
    }

    @EventListener
    public void handleRequestExpired(RideRequestExpiredEvent event) {
        RideRequest request = event.getRideRequest();
        log.info("Handling RideRequestExpiredEvent for request: {}", request.getId());

        RequestExpiredMessage payload = RequestExpiredMessage.builder()
                .eventType("REQUEST_EXPIRED")
                .requestId(request.getId())
                .build();

        redisPublisher.publish("REQUEST_EXPIRED", request.getId(), null, payload);
    }

    @EventListener
    public void handleRequestCancelled(RideRequestCancelledEvent event) {
        RideRequest request = event.getRideRequest();
        log.info("Handling RideRequestCancelledEvent for request: {}", request.getId());

        RequestCancelledMessage payload = RequestCancelledMessage.builder()
                .eventType("REQUEST_CANCELLED")
                .requestId(request.getId())
                .build();

        redisPublisher.publish("REQUEST_CANCELLED", request.getId(), null, payload);
    }
}
