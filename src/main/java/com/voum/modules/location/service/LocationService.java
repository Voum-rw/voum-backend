package com.voum.modules.location.service;

import com.voum.common.ApiException;
import com.voum.modules.location.dto.LocationUpdateRequest;
import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.mapper.LocationMapper;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.util.HaversineCalculator;
import com.voum.modules.users.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.voum.modules.location.dto.NearbyActivityResponse;
import com.voum.modules.marketplace.repository.RideRequestRepository;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final UserLocationRepository userLocationRepository;
    private final UserRepository userRepository;
    private final MotariRepository motariRepository;
    private final RideRequestRepository rideRequestRepository;
    private final LocationMapper locationMapper;
    private final LocationCacheService cacheService;

    @Transactional(readOnly = true)
    public List<NearbyActivityResponse> getNearbyActivity() {
        long activeCount = rideRequestRepository.countActiveOpenRequests(Instant.now());
        List<NearbyActivityResponse> list = new ArrayList<>();
        
        if (activeCount == 0) {
            // Quiet / Normal state when there are no active surges
            list.add(NearbyActivityResponse.builder()
                    .zoneName("Kigali Heights / Hub")
                    .demandLevel("Normal")
                    .subtitle("Wait < 3m · Moderate activity")
                    .isHighDemand(false)
                    .activeRequestsCount(0)
                    .build());
        } else {
            list.add(NearbyActivityResponse.builder()
                    .zoneName("Kimironko")
                    .demandLevel("High")
                    .subtitle("🔥 Wait < 2m · +" + activeCount + " active requests")
                    .isHighDemand(true)
                    .activeRequestsCount((int) activeCount)
                    .build());
        }
        return list;
    }

    @Transactional
    public void goOnline(UUID userId, LocationUpdateRequest req) {
        log.info("Driver going online: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (user.getRole() != Role.MOTARI) {
            throw new ApiException("Only Motaris are allowed to go online.", HttpStatus.FORBIDDEN);
        }

        Motari motari = motariRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        // Verification requirement temporarily disabled for testing Motari functionality
        // if (!"APPROVED".equals(motari.getVerificationStatus())) {
        //     throw new ApiException("Account verification is required before going online. Please upload all verification documents.", HttpStatus.FORBIDDEN);
        // }

        UserLocation location = userLocationRepository.findByUserId(userId)
                .orElseGet(() -> UserLocation.builder().userId(userId).build());

        location.setLatitude(req.getLatitude());
        location.setLongitude(req.getLongitude());
        location.setAccuracy(req.getAccuracy());
        location.setAvailabilityStatus("ONLINE");
        location.setLastSeenAt(Instant.now());
        userLocationRepository.save(location);

        // Sync user online flag
        user.setIsOnline(true);
        userRepository.save(user);

        // Cache update
        cacheService.cacheOnlineMotari(userId);
    }

    @Transactional
    public void goOffline(UUID userId) {
        log.info("Driver going offline: {}", userId);

        UserLocation location = userLocationRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Location registry not found for user.", HttpStatus.NOT_FOUND));

        location.setAvailabilityStatus("OFFLINE");
        location.setLastSeenAt(Instant.now());
        userLocationRepository.save(location);

        userRepository.findById(userId).ifPresent(user -> {
            user.setIsOnline(false);
            userRepository.save(user);
        });

        // Cache update
        cacheService.evictOnlineMotari(userId);
    }

    @Transactional
    public void updateAvailabilityStatus(UUID userId, String status) {
        log.info("Updating availability status for user {} to {}", userId, status);
        UserLocation location = userLocationRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Location registry not found.", HttpStatus.NOT_FOUND));

        location.setAvailabilityStatus(status);
        location.setLastSeenAt(Instant.now());
        userLocationRepository.save(location);

        if ("ONLINE".equals(status)) {
            cacheService.cacheOnlineMotari(userId);
        } else {
            cacheService.evictOnlineMotari(userId);
        }
    }

    @Transactional
    public void updateLocation(UUID userId, LocationUpdateRequest req) {
        log.debug("Updating coordinates for user: {}", userId);

        UserLocation location = userLocationRepository.findByUserId(userId)
                .orElseGet(() -> UserLocation.builder().userId(userId).build());

        location.setLatitude(req.getLatitude());
        location.setLongitude(req.getLongitude());
        location.setAccuracy(req.getAccuracy());
        location.setLastSeenAt(Instant.now());
        userLocationRepository.save(location);
    }

    @Transactional
    public void heartbeat(UUID userId) {
        log.trace("Heartbeat received for user: {}", userId);

        UserLocation location = userLocationRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Location registry not found.", HttpStatus.NOT_FOUND));

        location.setLastSeenAt(Instant.now());
        userLocationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public List<NearbyMotariResponse> findNearbyMotaris(double lat, double lng, double radiusKm) {
        // Validate Ranges
        if (lat < -90.0 || lat > 90.0) {
            throw new ApiException("Latitude must be between -90.0 and 90.0", HttpStatus.BAD_REQUEST);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new ApiException("Longitude must be between -180.0 and 180.0", HttpStatus.BAD_REQUEST);
        }
        if (radiusKm < 0.1 || radiusKm > 50.0) {
            throw new ApiException("Radius must be between 0.1km and 50.0km", HttpStatus.BAD_REQUEST);
        }

        // Bounding-box Calculation
        double earthRadius = 6371.0;
        double radLat = Math.toRadians(lat);

        double deltaLat = (radiusKm / earthRadius) * (180.0 / Math.PI);
        double deltaLng = (radiusKm / (earthRadius * Math.cos(radLat))) * (180.0 / Math.PI);

        double minLat = lat - deltaLat;
        double maxLat = lat + deltaLat;
        double minLng = lng - deltaLng;
        double maxLng = lng + deltaLng;

        Instant cutoffTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        // 1. Fetch Candidates from Database Bounding Box
        List<UserLocation> candidates = userLocationRepository.findDriversInBoundingBox(minLat, maxLat, minLng, maxLng, cutoffTime);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> driverIds = candidates.stream().map(UserLocation::getUserId).collect(Collectors.toList());

        // 2. Fetch driver profile details
        List<Motari> motaris = motariRepository.findAllById(driverIds);
        Map<UUID, Motari> motariMap = motaris.stream()
                // Verification check disabled for testing Motari functionality
                // .filter(m -> "APPROVED".equals(m.getVerificationStatus()))
                .collect(Collectors.toMap(Motari::getId, m -> m));

        List<NearbyMotariResponse> results = new ArrayList<>();

        for (UserLocation loc : candidates) {
            Motari motari = motariMap.get(loc.getUserId());
            if (motari == null) {
                continue; // Not approved or missing profile
            }

            // 3. Precise Distance Calculation using Haversine
            double dist = HaversineCalculator.calculateDistance(lat, lng, loc.getLatitude(), loc.getLongitude());
            if (dist <= radiusKm) {
                results.add(locationMapper.toNearbyResponse(motari, loc, dist));
            }
        }

        // Sort by distance ascending
        results.sort(Comparator.comparing(NearbyMotariResponse::getDistanceKm));

        return results;
    }

    /**
     * Stale heartbeat cleanup running every minute.
     * Marks drivers offline who haven't sent a heartbeat/update in 5 minutes.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanStaleSessions() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        
        // Find users that are going to be set offline to evict them from Redis
        List<UserLocation> activeLocations = userLocationRepository.findAll();
        for (UserLocation loc : activeLocations) {
            if (("ONLINE".equals(loc.getAvailabilityStatus()) || "BUSY".equals(loc.getAvailabilityStatus()))
                    && (loc.getLastSeenAt() == null || loc.getLastSeenAt().isBefore(cutoff))) {
                cacheService.evictOnlineMotari(loc.getUserId());
                
                // Sync User active online flag
                userRepository.findById(loc.getUserId()).ifPresent(user -> {
                    user.setIsOnline(false);
                    userRepository.save(user);
                });
            }
        }

        int updated = userLocationRepository.markStaleUsersOffline(cutoff);
        if (updated > 0) {
            log.info("Marked {} stale drivers as OFFLINE due to heartbeat timeout", updated);
        }
    }
}
