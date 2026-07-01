package com.voum.modules.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.common.ApiException;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.util.HaversineCalculator;
import com.voum.modules.tracking.dto.LocationUpdateRequest;
import com.voum.modules.tracking.dto.TripEtaResponse;
import com.voum.modules.tracking.dto.TripLocationResponse;
import com.voum.modules.tracking.entity.TripTrackingPoint;
import com.voum.modules.tracking.events.TripLocationUpdatedEvent;
import com.voum.modules.tracking.repository.TripTrackingPointRepository;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripTrackingService {

    private static final Logger log = LoggerFactory.getLogger(TripTrackingService.class);

    private static final String REDIS_LATEST_KEY_PREFIX = "voum:trip:tracking:latest:";
    private static final String REDIS_LAST_SAVED_KEY_PREFIX = "voum:trip:tracking:last_saved:";
    private static final String REDIS_LAST_UPDATE_KEY_PREFIX = "voum:trip:tracking:last_update:";
    private static final String REDIS_ETA_KEY_PREFIX = "voum:trip:eta:";
    private static final String REDIS_SEQ_KEY_PREFIX = "voum:trip:tracking:seq:";

    private final TripRepository tripRepository;
    private final TripTrackingPointRepository tripTrackingPointRepository;
    private final UserLocationRepository userLocationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TripLocationResponse updateLocation(UUID tripId, UUID driverId, LocationUpdateRequest req) {
        log.debug("Receiving location update for trip: {} from driver: {}", tripId, driverId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Trip not found.", HttpStatus.NOT_FOUND));

        // 1. Verify assigned Motari
        if (!trip.getMotariId().equals(driverId)) {
            throw new ApiException("Access Denied: You are not the driver assigned to this trip.", HttpStatus.FORBIDDEN);
        }

        // 2. Verify active trip status
        String status = trip.getStatus();
        if (!List.of("MOTARI_EN_ROUTE", "MOTARI_ARRIVED", "PASSENGER_ONBOARD", "IN_PROGRESS").contains(status)) {
            throw new ApiException("Location updates are only allowed for active trips.", HttpStatus.BAD_REQUEST);
        }

        // 3. Verify Motari availability status must be BUSY
        UserLocation driverLoc = userLocationRepository.findByUserId(driverId)
                .orElseThrow(() -> new ApiException("Driver location configuration not found.", HttpStatus.BAD_REQUEST));
        if (!"BUSY".equalsIgnoreCase(driverLoc.getAvailabilityStatus())) {
            throw new ApiException("Location updates are only allowed when your status is marked as BUSY.", HttpStatus.BAD_REQUEST);
        }

        // 4. Rate Limiting: Max 1 update per 2 seconds
        String lastUpdateKey = REDIS_LAST_UPDATE_KEY_PREFIX + tripId;
        String lastUpdateVal = redisTemplate.opsForValue().get(lastUpdateKey);
        Instant now = Instant.now();
        if (lastUpdateVal != null) {
            long lastUpdateEpoch = Long.parseLong(lastUpdateVal);
            if (now.toEpochMilli() - lastUpdateEpoch < 2000) {
                log.warn("Rate limit triggered for trip location updates on trip: {}", tripId);
                throw new ApiException("Too many updates. Please wait at least 2 seconds between updates.", HttpStatus.TOO_MANY_REQUESTS);
            }
        }
        redisTemplate.opsForValue().set(lastUpdateKey, String.valueOf(now.toEpochMilli()), Duration.ofSeconds(10));

        // 5. Update Trip entity current location
        trip.setCurrentLatitude(req.getLatitude());
        trip.setCurrentLongitude(req.getLongitude());
        trip.setLastLocationUpdateAt(now);
        tripRepository.save(trip);

        // 6. Cache latest position in Redis
        TripLocationResponse currentResponse = TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .accuracy(req.getAccuracy())
                .speedKmh(req.getSpeedKmh())
                .headingDegrees(req.getHeadingDegrees())
                .batteryLevel(req.getBatteryLevel())
                .gpsMocked(req.getGpsMocked())
                .recordedAt(now)
                .build();

        try {
            redisTemplate.opsForValue().set(
                    REDIS_LATEST_KEY_PREFIX + tripId,
                    objectMapper.writeValueAsString(currentResponse),
                    Duration.ofMinutes(5)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to cache latest location for trip: {}", tripId, e);
        }

        // 7. Write Reduction Throttling for History DB Table
        TripLocationResponse lastSaved = getLastSavedLocation(tripId);
        boolean shouldSave = false;

        if (lastSaved == null) {
            shouldSave = true;
        } else {
            double distanceKm = HaversineCalculator.calculateDistance(
                    lastSaved.getLatitude(), lastSaved.getLongitude(),
                    req.getLatitude(), req.getLongitude()
            );
            long secondsElapsed = now.getEpochSecond() - lastSaved.getRecordedAt().getEpochSecond();
            
            // Save if moved > 10 meters OR if 15 seconds elapsed
            if (distanceKm > 0.010 || secondsElapsed >= 15) {
                shouldSave = true;
            }
        }

        if (shouldSave) {
            // Generate sequence number using Redis increment
            Long seq = redisTemplate.opsForValue().increment(REDIS_SEQ_KEY_PREFIX + tripId);
            if (seq == null) {
                seq = 1L;
            }
            currentResponse.setSequenceNumber(seq);

            TripTrackingPoint trackingPoint = TripTrackingPoint.builder()
                    .tripId(tripId)
                    .sequenceNumber(seq)
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .accuracy(req.getAccuracy())
                    .speedKmh(req.getSpeedKmh())
                    .headingDegrees(req.getHeadingDegrees())
                    .batteryLevel(req.getBatteryLevel())
                    .gpsMocked(req.getGpsMocked())
                    .recordedAt(now)
                    .build();

            tripTrackingPointRepository.save(trackingPoint);

            // Cache the newly saved point in Redis
            try {
                redisTemplate.opsForValue().set(
                        REDIS_LAST_SAVED_KEY_PREFIX + tripId,
                        objectMapper.writeValueAsString(currentResponse),
                        Duration.ofMinutes(5)
                );
            } catch (JsonProcessingException e) {
                log.error("Failed to cache last saved location for trip: {}", tripId, e);
            }
        }

        // 8. Publish Spring Event for real-time WebSocket broadcasting
        eventPublisher.publishEvent(new TripLocationUpdatedEvent(
                this, tripId, req.getLatitude(), req.getLongitude(), req.getSpeedKmh(), now
        ));

        // Evict cached ETA since location has changed
        redisTemplate.delete(REDIS_ETA_KEY_PREFIX + tripId);

        return currentResponse;
    }

    @Transactional(readOnly = true)
    public TripLocationResponse getCurrentLocation(UUID tripId, UUID userId) {
        Trip trip = findAndValidateTripOwnership(tripId, userId);

        // Try reading from Redis cache
        String latestJson = redisTemplate.opsForValue().get(REDIS_LATEST_KEY_PREFIX + tripId);
        if (latestJson != null) {
            try {
                return objectMapper.readValue(latestJson, TripLocationResponse.class);
            } catch (IOException e) {
                log.error("Failed to deserialize cached location for trip: {}", tripId, e);
            }
        }

        // Fallback to Trip database columns
        if (trip.getCurrentLatitude() == null || trip.getCurrentLongitude() == null) {
            throw new ApiException("No location updates recorded yet.", HttpStatus.NOT_FOUND);
        }

        return TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(trip.getCurrentLatitude())
                .longitude(trip.getCurrentLongitude())
                .recordedAt(trip.getLastLocationUpdateAt() != null ? trip.getLastLocationUpdateAt() : trip.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TripLocationResponse> getLocationHistory(UUID tripId, UUID userId) {
        findAndValidateTripOwnership(tripId, userId);

        return tripTrackingPointRepository.findTop100ByTripIdOrderBySequenceNumberDesc(tripId).stream()
                .map(point -> TripLocationResponse.builder()
                        .tripId(point.getTripId())
                        .sequenceNumber(point.getSequenceNumber())
                        .latitude(point.getLatitude())
                        .longitude(point.getLongitude())
                        .accuracy(point.getAccuracy())
                        .speedKmh(point.getSpeedKmh())
                        .headingDegrees(point.getHeadingDegrees())
                        .batteryLevel(point.getBatteryLevel())
                        .gpsMocked(point.getGpsMocked())
                        .recordedAt(point.getRecordedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public TripEtaResponse getEta(UUID tripId, UUID userId) {
        Trip trip = findAndValidateTripOwnership(tripId, userId);

        String etaKey = REDIS_ETA_KEY_PREFIX + tripId;
        String cachedEta = redisTemplate.opsForValue().get(etaKey);
        if (cachedEta != null) {
            return new TripEtaResponse(Integer.parseInt(cachedEta));
        }

        // Resolve current coordinates
        Double currentLat = null;
        Double currentLng = null;
        Double speedKmh = null;

        // Try cached latest
        String latestJson = redisTemplate.opsForValue().get(REDIS_LATEST_KEY_PREFIX + tripId);
        if (latestJson != null) {
            try {
                TripLocationResponse res = objectMapper.readValue(latestJson, TripLocationResponse.class);
                currentLat = res.getLatitude();
                currentLng = res.getLongitude();
                speedKmh = res.getSpeedKmh();
            } catch (IOException e) {
                log.error("Failed to read latest location for ETA estimation.", e);
            }
        }

        // Try Trip fields
        if (currentLat == null || currentLng == null) {
            currentLat = trip.getCurrentLatitude();
            currentLng = trip.getCurrentLongitude();
        }

        // Try Motari location table fallback
        if (currentLat == null || currentLng == null) {
            UserLocation driverLoc = userLocationRepository.findByUserId(trip.getMotariId()).orElse(null);
            if (driverLoc != null) {
                currentLat = driverLoc.getLatitude();
                currentLng = driverLoc.getLongitude();
            }
        }

        if (currentLat == null || currentLng == null) {
            throw new ApiException("Driver location is not available.", HttpStatus.BAD_REQUEST);
        }

        // Resolve target coordinates based on status
        String status = trip.getStatus();
        Double targetLat;
        Double targetLng;

        if (List.of("MOTARI_EN_ROUTE", "MOTARI_ARRIVED").contains(status)) {
            targetLat = trip.getPickupLatitude();
            targetLng = trip.getPickupLongitude();
        } else if ("IN_PROGRESS".equals(status)) {
            targetLat = trip.getDestinationLatitude();
            targetLng = trip.getDestinationLongitude();
        } else {
            throw new ApiException("ETA calculation is only available for active trips.", HttpStatus.BAD_REQUEST);
        }

        double distanceKm = HaversineCalculator.calculateDistance(currentLat, currentLng, targetLat, targetLng);

        // Compute ETA
        double speed = 30.0; // default average speed in km/h
        if (speedKmh != null && speedKmh > 5.0) {
            speed = speedKmh;
        }

        double hours = distanceKm / speed;
        int estimatedMinutes = (int) Math.ceil(hours * 60.0);

        // Clamp values between 1 and 120 minutes
        if (estimatedMinutes < 1) {
            estimatedMinutes = 1;
        } else if (estimatedMinutes > 120) {
            estimatedMinutes = 120;
        }

        redisTemplate.opsForValue().set(etaKey, String.valueOf(estimatedMinutes), Duration.ofMinutes(5));

        return new TripEtaResponse(estimatedMinutes);
    }

    // Proximity auto-detection utilities
    public boolean isNearPickup(Trip trip, double currentLat, double currentLng) {
        double dist = HaversineCalculator.calculateDistance(currentLat, currentLng, trip.getPickupLatitude(), trip.getPickupLongitude());
        return dist < 0.030; // 30 meters
    }

    public boolean isNearDestination(Trip trip, double currentLat, double currentLng) {
        double dist = HaversineCalculator.calculateDistance(currentLat, currentLng, trip.getDestinationLatitude(), trip.getDestinationLongitude());
        return dist < 0.030; // 30 meters
    }

    private Trip findAndValidateTripOwnership(UUID tripId, UUID userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Trip not found.", HttpStatus.NOT_FOUND));

        if (!trip.getPassengerId().equals(userId) && !trip.getMotariId().equals(userId)) {
            throw new ApiException("Access Denied: You are not assigned to this trip.", HttpStatus.FORBIDDEN);
        }

        return trip;
    }

    private TripLocationResponse getLastSavedLocation(UUID tripId) {
        String lastSavedJson = redisTemplate.opsForValue().get(REDIS_LAST_SAVED_KEY_PREFIX + tripId);
        if (lastSavedJson != null) {
            try {
                return objectMapper.readValue(lastSavedJson, TripLocationResponse.class);
            } catch (IOException e) {
                log.error("Failed to parse cached last saved location.", e);
            }
        }

        // Fallback to DB
        List<TripTrackingPoint> points = tripTrackingPointRepository.findTop100ByTripIdOrderBySequenceNumberDesc(tripId);
        if (!points.isEmpty()) {
            TripTrackingPoint p = points.get(0);
            return TripLocationResponse.builder()
                    .tripId(tripId)
                    .sequenceNumber(p.getSequenceNumber())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .accuracy(p.getAccuracy())
                    .speedKmh(p.getSpeedKmh())
                    .headingDegrees(p.getHeadingDegrees())
                    .batteryLevel(p.getBatteryLevel())
                    .gpsMocked(p.getGpsMocked())
                    .recordedAt(p.getRecordedAt())
                    .build();
        }

        return null;
    }
}
