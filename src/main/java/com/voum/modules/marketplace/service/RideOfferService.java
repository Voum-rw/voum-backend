package com.voum.modules.marketplace.service;

import com.voum.common.ApiException;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.location.util.HaversineCalculator;
import com.voum.modules.marketplace.dto.RideOfferCreateRequest;
import com.voum.modules.marketplace.dto.RideOfferResponse;
import com.voum.modules.marketplace.dto.RideOfferUpdateRequest;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.RideOfferAcceptedEvent;
import com.voum.modules.marketplace.events.RideOfferSubmittedEvent;
import com.voum.modules.marketplace.mapper.MarketplaceMapper;
import com.voum.modules.marketplace.repository.RideOfferRepository;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.users.Motari;
import com.voum.modules.users.MotariRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideOfferService {

    private static final Logger log = LoggerFactory.getLogger(RideOfferService.class);

    private final RideOfferRepository rideOfferRepository;
    private final RideRequestRepository rideRequestRepository;
    private final MotariRepository motariRepository;
    private final UserLocationRepository userLocationRepository;
    private final LocationService locationService;
    private final MarketplaceMapper marketplaceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RideOfferResponse submitOffer(UUID motariId, RideOfferCreateRequest req) {
        log.info("Motari [{}] submitting offer for request: {}", motariId, req.getRideRequestId());

        // Validate Motari is approved
        Motari motari = motariRepository.findById(motariId)
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        if (!"APPROVED".equals(motari.getVerificationStatus())) {
            throw new ApiException("Only approved drivers can submit offers.", HttpStatus.FORBIDDEN);
        }

        // Validate Motari presence (ONLINE and not BUSY)
        UserLocation driverLoc = userLocationRepository.findByUserId(motariId)
                .orElseThrow(() -> new ApiException("Driver location coordinates not found.", HttpStatus.BAD_REQUEST));

        if (!"ONLINE".equals(driverLoc.getAvailabilityStatus())) {
            throw new ApiException("You must be ONLINE and not BUSY to submit ride offers.", HttpStatus.FORBIDDEN);
        }

        // Validate Request is active and OPEN
        RideRequest request = rideRequestRepository.findById(req.getRideRequestId())
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));

        if (!"OPEN".equals(request.getStatus()) || request.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Ride request is no longer open for offers.", HttpStatus.BAD_REQUEST);
        }

        // Rule: One ACTIVE offer per Motari per request
        boolean alreadyOffered = rideOfferRepository.existsByRideRequestIdAndMotariIdAndStatus(
                req.getRideRequestId(), motariId, "PENDING"
        );
        if (alreadyOffered) {
            throw new ApiException("You have already submitted an active offer for this request. Please update your existing offer instead.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        RideOffer offer = RideOffer.builder()
                .rideRequestId(req.getRideRequestId())
                .motariId(motariId)
                .offeredPrice(req.getOfferedPrice())
                .estimatedArrivalMinutes(req.getEstimatedArrivalMinutes())
                .status("PENDING")
                .updateCount(0)
                .createdAt(now)   // set explicitly so it's available on the returned entity immediately
                .build();

        offer = rideOfferRepository.save(offer);

        // Increment offers_count
        request.setOffersCount(request.getOffersCount() + 1);
        rideRequestRepository.save(request);

        // Publish domain event
        eventPublisher.publishEvent(new RideOfferSubmittedEvent(this, offer));

        double distance = HaversineCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                driverLoc.getLatitude(), driverLoc.getLongitude()
        );

        return marketplaceMapper.toOfferResponse(offer, motari, distance);
    }

    @Transactional
    public RideOfferResponse updateOffer(UUID motariId, UUID offerId, RideOfferUpdateRequest req) {
        log.info("Motari [{}] updating offer: {}", motariId, offerId);

        RideOffer offer = rideOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException("Ride offer not found.", HttpStatus.NOT_FOUND));

        if (!offer.getMotariId().equals(motariId)) {
            throw new ApiException("Access denied.", HttpStatus.FORBIDDEN);
        }

        if (!"PENDING".equals(offer.getStatus())) {
            throw new ApiException("Only pending offers can be updated.", HttpStatus.BAD_REQUEST);
        }

        RideRequest request = rideRequestRepository.findById(offer.getRideRequestId())
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));

        if (!"OPEN".equals(request.getStatus()) || request.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Ride request has expired or is no longer open.", HttpStatus.BAD_REQUEST);
        }

        // Rule: Maximum 5 updates
        if (offer.getUpdateCount() >= 5) {
            throw new ApiException("Maximum price updates limit (5) reached for this offer.", HttpStatus.BAD_REQUEST);
        }

        offer.setOfferedPrice(req.getOfferedPrice());
        offer.setEstimatedArrivalMinutes(req.getEstimatedArrivalMinutes());
        offer.setUpdateCount(offer.getUpdateCount() + 1);
        offer = rideOfferRepository.save(offer);

        // Increment request version to track updates
        request.setRequestVersion(request.getRequestVersion() + 1);
        rideRequestRepository.save(request);

        UserLocation driverLoc = userLocationRepository.findByUserId(motariId).orElse(null);
        double distance = 0.0;
        if (driverLoc != null) {
            distance = HaversineCalculator.calculateDistance(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    driverLoc.getLatitude(), driverLoc.getLongitude()
            );
        }

        Motari motari = motariRepository.findById(motariId).orElse(null);
        return marketplaceMapper.toOfferResponse(offer, motari, distance);
    }

    @Transactional
    public void withdrawOffer(UUID motariId, UUID offerId) {
        log.info("Motari [{}] withdrawing offer: {}", motariId, offerId);

        RideOffer offer = rideOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException("Ride offer not found.", HttpStatus.NOT_FOUND));

        if (!offer.getMotariId().equals(motariId)) {
            throw new ApiException("Access denied.", HttpStatus.FORBIDDEN);
        }

        if (!"PENDING".equals(offer.getStatus())) {
            throw new ApiException("Only pending offers can be withdrawn.", HttpStatus.BAD_REQUEST);
        }

        offer.setStatus("WITHDRAWN");
        rideOfferRepository.save(offer);

        RideRequest request = rideRequestRepository.findById(offer.getRideRequestId()).orElse(null);
        if (request != null && request.getOffersCount() > 0) {
            request.setOffersCount(request.getOffersCount() - 1);
            rideRequestRepository.save(request);
        }
    }

    @Transactional(readOnly = true)
    public List<RideOfferResponse> getOffersForRequest(UUID passengerId, UUID requestId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));

        if (!request.getPassengerId().equals(passengerId)) {
            throw new ApiException("Access denied.", HttpStatus.FORBIDDEN);
        }

        List<RideOffer> offers = rideOfferRepository.findByRideRequestId(requestId);
        if (offers.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> driverIds = offers.stream().map(RideOffer::getMotariId).collect(Collectors.toList());
        List<Motari> motaris = motariRepository.findAllById(driverIds);
        Map<UUID, Motari> motariMap = motaris.stream().collect(Collectors.toMap(Motari::getId, m -> m));

        List<UserLocation> locations = userLocationRepository.findAll();
        Map<UUID, UserLocation> locMap = locations.stream().collect(Collectors.toMap(UserLocation::getUserId, l -> l));

        List<RideOfferResponse> responses = new ArrayList<>();
        for (RideOffer offer : offers) {
            Motari motari = motariMap.get(offer.getMotariId());
            UserLocation loc = locMap.get(offer.getMotariId());
            double dist = 0.0;
            if (loc != null) {
                dist = HaversineCalculator.calculateDistance(
                        request.getPickupLatitude(), request.getPickupLongitude(),
                        loc.getLatitude(), loc.getLongitude()
                );
            }
            responses.add(marketplaceMapper.toOfferResponse(offer, motari, dist));
        }

        // Sorting: Price first (ascending), Distance second (ascending), Response time third (createdAt ascending)
        responses.sort(Comparator.comparing(RideOfferResponse::getOfferedPrice)
                .thenComparing(RideOfferResponse::getDistanceKm)
                .thenComparing(o -> {
                    RideOffer origOffer = offers.stream().filter(of -> of.getId().equals(o.getId())).findFirst().orElse(null);
                    return origOffer != null ? origOffer.getCreatedAt() : Instant.now();
                })
        );

        return responses;
    }

    @Transactional
    public void acceptOffer(UUID passengerId, UUID offerId) {
        log.info("Passenger [{}] accepting offer: {}", passengerId, offerId);

        RideOffer selectedOffer = rideOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException("Ride offer not found.", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(selectedOffer.getStatus())) {
            throw new ApiException("Offer is no longer active.", HttpStatus.BAD_REQUEST);
        }

        RideRequest request = rideRequestRepository.findById(selectedOffer.getRideRequestId())
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));

        if (!request.getPassengerId().equals(passengerId)) {
            throw new ApiException("Access denied.", HttpStatus.FORBIDDEN);
        }

        if (!"OPEN".equals(request.getStatus()) || request.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Ride request has expired or is no longer open.", HttpStatus.BAD_REQUEST);
        }

        // 1. Accept Selected Offer
        selectedOffer.setStatus("ACCEPTED");
        rideOfferRepository.save(selectedOffer);

        // 2. Reject other active offers for this request
        List<RideOffer> otherOffers = rideOfferRepository.findByRideRequestIdAndStatus(request.getId(), "PENDING");
        for (RideOffer other : otherOffers) {
            if (!other.getId().equals(offerId)) {
                other.setStatus("REJECTED");
                rideOfferRepository.save(other);
            }
        }

        // 3. Match Request
        request.setStatus("MATCHED");
        request.setSelectedOfferId(offerId);
        rideRequestRepository.save(request);

        // 4. Update Motari availability status to BUSY
        locationService.updateAvailabilityStatus(selectedOffer.getMotariId(), "BUSY");

        // Publish domain event
        eventPublisher.publishEvent(new RideOfferAcceptedEvent(this, request, selectedOffer));
    }
}
