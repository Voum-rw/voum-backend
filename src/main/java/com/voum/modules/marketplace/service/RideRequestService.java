package com.voum.modules.marketplace.service;

import com.voum.common.ApiException;
import com.voum.modules.location.util.HaversineCalculator;
import com.voum.modules.marketplace.dto.RideRequestCreateRequest;
import com.voum.modules.marketplace.dto.RideRequestResponse;
import com.voum.modules.marketplace.dto.RideRequestStatusResponse;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.RideRequestCreatedEvent;
import com.voum.modules.marketplace.events.RideRequestExpiredEvent;
import com.voum.modules.marketplace.mapper.MarketplaceMapper;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.users.PassengerRepository;
import com.voum.modules.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideRequestService {

    private static final Logger log = LoggerFactory.getLogger(RideRequestService.class);

    private final RideRequestRepository rideRequestRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MarketplaceMapper marketplaceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RideRequestResponse createRequest(UUID passengerId, RideRequestCreateRequest req) {
        log.info("Passenger creating ride request: {}", passengerId);

        // Validate passenger exists
        if (!passengerRepository.existsById(passengerId)) {
            throw new ApiException("Passenger profile not found.", HttpStatus.NOT_FOUND);
        }

        // Rule: One ACTIVE request per passenger at a time
        Instant now = Instant.now();
        List<RideRequest> existingRequests = rideRequestRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId);
        for (RideRequest reqItem : existingRequests) {
            if ("OPEN".equals(reqItem.getStatus())) {
                if (reqItem.getExpiresAt() != null && reqItem.getExpiresAt().isBefore(now)) {
                    reqItem.setStatus("EXPIRED");
                    rideRequestRepository.save(reqItem);
                } else {
                    throw new ApiException("You already have an active open ride request. Please cancel or wait for it to expire.", HttpStatus.BAD_REQUEST);
                }
            }
        }

        Instant expiresAt = now.plus(Duration.ofMinutes(5)); // 5 minutes visibility window for testing

        RideRequest request = RideRequest.builder()
                .passengerId(passengerId)
                .pickupLatitude(req.getPickupLatitude())
                .pickupLongitude(req.getPickupLongitude())
                .destinationLatitude(req.getDestinationLatitude())
                .destinationLongitude(req.getDestinationLongitude())
                .pickupAddress(req.getPickupAddress())
                .destinationAddress(req.getDestinationAddress())
                .proposedBudget(req.getProposedBudget())
                .visibilityRadiusKm(req.getVisibilityRadiusKm() != null ? req.getVisibilityRadiusKm() : 3.00)
                .createdArea(req.getCreatedArea())
                .expiresAt(expiresAt)
                .status("OPEN")
                .offersCount(0)
                .requestVersion(1)
                .createdAt(now)   // set explicitly so it's available on the returned entity immediately
                .updatedAt(now)
                .build();

        request = rideRequestRepository.save(request);

        // Publish domain event
        eventPublisher.publishEvent(new RideRequestCreatedEvent(this, request));

        return marketplaceMapper.toRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public RideRequestResponse getActiveRequest(UUID passengerId) {
        Instant now = Instant.now();
        List<RideRequest> requests = rideRequestRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId);
        for (RideRequest req : requests) {
            if ("OPEN".equals(req.getStatus()) && req.getExpiresAt() != null && req.getExpiresAt().isAfter(now)) {
                return marketplaceMapper.toRequestResponse(req);
            }
        }
        return null;
    }

    @Transactional
    public void cancelRequest(UUID id, UUID passengerId) {
        RideRequest request = rideRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));

        if (!request.getPassengerId().equals(passengerId)) {
            throw new ApiException("You are not authorized to cancel this request.", HttpStatus.FORBIDDEN);
        }

        request.setStatus("CANCELLED");
        rideRequestRepository.save(request);
        log.info("Ride request {} cancelled by passenger {}", id, passengerId);
    }


    @Transactional(readOnly = true)
    public RideRequestResponse getRequest(UUID id) {
        RideRequest request = rideRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));
        return marketplaceMapper.toRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public RideRequestStatusResponse getRequestStatus(UUID id) {
        RideRequest request = rideRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Ride request not found.", HttpStatus.NOT_FOUND));

        long secondsRemaining = Math.max(0, Duration.between(Instant.now(), request.getExpiresAt()).toSeconds());
        if (!"OPEN".equals(request.getStatus())) {
            secondsRemaining = 0;
        }

        return RideRequestStatusResponse.builder()
                .status(request.getStatus())
                .offersCount(request.getOffersCount())
                .secondsRemaining(secondsRemaining)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RideRequestResponse> getMyRequests(UUID passengerId) {
        return rideRequestRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId).stream()
                .map(marketplaceMapper::toRequestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RideRequestResponse> findNearbyRequests(double lat, double lng, double radiusKm) {
        // Validate coordinates & search radius
        if (lat < -90.0 || lat > 90.0) {
            throw new ApiException("Latitude must be between -90.0 and 90.0", HttpStatus.BAD_REQUEST);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new ApiException("Longitude must be between -180.0 and 180.0", HttpStatus.BAD_REQUEST);
        }
        if (radiusKm < 0.1 || radiusKm > 50.0) {
            throw new ApiException("Radius must be between 0.1km and 50.0km", HttpStatus.BAD_REQUEST);
        }

        // Bounding-box Calculation (expanded to 10km for city-wide test coverage)
        double searchRadius = Math.max(radiusKm, 10.0);
        double earthRadius = 6371.0;
        double radLat = Math.toRadians(lat);

        double deltaLat = (searchRadius / earthRadius) * (180.0 / Math.PI);
        double deltaLng = (searchRadius / (earthRadius * Math.cos(radLat))) * (180.0 / Math.PI);

        double minLat = lat - deltaLat;
        double maxLat = lat + deltaLat;
        double minLng = lng - deltaLng;
        double maxLng = lng + deltaLng;

        Instant now = Instant.now();

        // 1. Fetch Candidates from Database Bounding Box
        List<RideRequest> candidates = rideRequestRepository.findOpenRequestsInBoundingBox(minLat, maxLat, minLng, maxLng, now);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<RideRequestResponse> results = new ArrayList<>();

        for (RideRequest req : candidates) {
            // 2. Precise Distance Calculation using Haversine
            double dist = HaversineCalculator.calculateDistance(lat, lng, req.getPickupLatitude(), req.getPickupLongitude());
            
            // Respect effective visibility radius (expanded to 10km for city-wide test coverage)
            double effectiveRadius = Math.max(req.getVisibilityRadiusKm() != null ? req.getVisibilityRadiusKm() : 3.0, 10.0);
            if (dist <= effectiveRadius) {
                results.add(marketplaceMapper.toRequestResponse(req));
            }
        }

        // Sort by distance (pickup latitude/longitude to driver) ascending
        results.sort(Comparator.comparing(r -> HaversineCalculator.calculateDistance(lat, lng, r.getPickupLatitude(), r.getPickupLongitude())));

        return results;
    }

    /**
     * Expiration task running every 10 seconds.
     * Transitions OPEN requests to EXPIRED if they past expiresAt.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void cleanExpiredRequests() {
        Instant now = Instant.now();
        List<RideRequest> expired = rideRequestRepository.findByStatusAndExpiresAtBefore("OPEN", now);
        
        for (RideRequest req : expired) {
            req.setStatus("EXPIRED");
            rideRequestRepository.save(req);
            
            log.info("Ride request expired: {}", req.getId());
            eventPublisher.publishEvent(new RideRequestExpiredEvent(this, req));
        }
    }
}
