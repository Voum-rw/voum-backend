package com.voum.modules.realtime.listeners;

import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.*;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.realtime.dto.*;
import com.voum.modules.realtime.publishers.RedisMessagePublisher;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.events.*;
import com.voum.modules.trip.mapper.TripMapper;
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
    private final TripMapper tripMapper;

    @EventListener
    public void handleRequestCreated(RideRequestCreatedEvent event) {
        RideRequest request = event.getRideRequest();
        log.info("Handling RideRequestCreatedEvent for request: {}", request.getId());

        // Find nearby online drivers within radius (expanded to 10km for city-wide test coverage)
        double radius = request.getVisibilityRadiusKm() != null ? Math.max(request.getVisibilityRadiusKm(), 10.0) : 10.0;
        List<NearbyMotariResponse> nearby = locationService.findNearbyMotaris(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                radius
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
        redisPublisher.publish("REQUEST_CREATED", null, null, driverIds, payload);
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
        redisPublisher.publish("NEW_OFFER", offer.getRideRequestId(), null, null, payload);
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

        redisPublisher.publish("OFFER_UPDATED", offer.getRideRequestId(), null, null, payload);
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

        redisPublisher.publish("OFFER_WITHDRAWN", offer.getRideRequestId(), null, null, payload);
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

        redisPublisher.publish("OFFER_ACCEPTED", request.getId(), null, null, payload);
    }

    @EventListener
    public void handleRequestExpired(RideRequestExpiredEvent event) {
        RideRequest request = event.getRideRequest();
        log.info("Handling RideRequestExpiredEvent for request: {}", request.getId());

        RequestExpiredMessage payload = RequestExpiredMessage.builder()
                .eventType("REQUEST_EXPIRED")
                .requestId(request.getId())
                .build();

        redisPublisher.publish("REQUEST_EXPIRED", request.getId(), null, null, payload);
    }

    @EventListener
    public void handleRequestCancelled(RideRequestCancelledEvent event) {
        RideRequest request = event.getRideRequest();
        log.info("Handling RideRequestCancelledEvent for request: {}", request.getId());

        RequestCancelledMessage payload = RequestCancelledMessage.builder()
                .eventType("REQUEST_CANCELLED")
                .requestId(request.getId())
                .build();

        redisPublisher.publish("REQUEST_CANCELLED", request.getId(), null, null, payload);
    }

    // ==========================================
    // Trip Lifecycle Listeners
    // ==========================================

    @EventListener
    public void handleTripCreated(TripCreatedEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting TripCreatedEvent for trip: {}", trip.getId());
        redisPublisher.publish("TRIP_CREATED", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handleMotariEnRoute(MotariEnRouteEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting MotariEnRouteEvent for trip: {}", trip.getId());
        redisPublisher.publish("MOTARI_EN_ROUTE", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handleMotariArrived(MotariArrivedEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting MotariArrivedEvent for trip: {}", trip.getId());
        redisPublisher.publish("MOTARI_ARRIVED", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handlePassengerBoarded(PassengerBoardedEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting PassengerBoardedEvent for trip: {}", trip.getId());
        redisPublisher.publish("PASSENGER_ONBOARD", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handleTripStarted(TripStartedEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting TripStartedEvent for trip: {}", trip.getId());
        redisPublisher.publish("IN_PROGRESS", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handleTripCompleted(TripCompletedEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting TripCompletedEvent for trip: {}", trip.getId());
        redisPublisher.publish("COMPLETED", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handleTripCancelled(TripCancelledEvent event) {
        Trip trip = event.getTrip();
        log.info("Broadcasting TripCancelledEvent for trip: {}", trip.getId());
        redisPublisher.publish("CANCELLED", null, trip.getId(), null, tripMapper.toResponse(trip));
    }

    @EventListener
    public void handleTripLocationUpdated(com.voum.modules.tracking.events.TripLocationUpdatedEvent event) {
        log.debug("Handling TripLocationUpdatedEvent for trip: {}", event.getTripId());

        TripLocationUpdatedMessage payload = TripLocationUpdatedMessage.builder()
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .speedKmh(event.getSpeedKmh())
                .recordedAt(event.getRecordedAt())
                .build();

        redisPublisher.publish("LOCATION_UPDATED", null, event.getTripId(), null, payload);
    }
}
