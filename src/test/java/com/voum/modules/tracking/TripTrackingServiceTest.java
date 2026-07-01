package com.voum.modules.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.common.ApiException;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.tracking.dto.LocationUpdateRequest;
import com.voum.modules.tracking.dto.TripEtaResponse;
import com.voum.modules.tracking.dto.TripLocationResponse;
import com.voum.modules.tracking.entity.TripTrackingPoint;
import com.voum.modules.tracking.events.TripLocationUpdatedEvent;
import com.voum.modules.tracking.repository.TripTrackingPointRepository;
import com.voum.modules.tracking.service.TripTrackingService;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TripTrackingServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripTrackingPointRepository tripTrackingPointRepository;

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private TripTrackingService trackingService;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Support Instant type serialization
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        trackingService = new TripTrackingService(
                tripRepository, tripTrackingPointRepository, userLocationRepository, redisTemplate, objectMapper, eventPublisher
        );
    }

    @Test
    public void testUpdateLocation_unassignedDriver_shouldThrowForbidden() {
        UUID tripId = UUID.randomUUID();
        UUID assignedDriverId = UUID.randomUUID();
        UUID otherDriverId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .motariId(assignedDriverId)
                .status("MOTARI_EN_ROUTE")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        LocationUpdateRequest req = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                trackingService.updateLocation(tripId, otherDriverId, req)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    public void testUpdateLocation_inactiveTripState_shouldThrowBadRequest() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .motariId(driverId)
                .status("COMPLETED")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        LocationUpdateRequest req = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                trackingService.updateLocation(tripId, driverId, req)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void testUpdateLocation_driverNotBusy_shouldThrowBadRequest() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .motariId(driverId)
                .status("MOTARI_EN_ROUTE")
                .build();

        UserLocation location = UserLocation.builder()
                .userId(driverId)
                .availabilityStatus("ONLINE") // Should be BUSY during a trip!
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userLocationRepository.findByUserId(driverId)).thenReturn(Optional.of(location));

        LocationUpdateRequest req = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                trackingService.updateLocation(tripId, driverId, req)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void testUpdateLocation_rateLimitTriggered_shouldThrowTooManyRequests() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .motariId(driverId)
                .status("MOTARI_EN_ROUTE")
                .build();

        UserLocation location = UserLocation.builder()
                .userId(driverId)
                .availabilityStatus("BUSY")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userLocationRepository.findByUserId(driverId)).thenReturn(Optional.of(location));
        
        // Mock that the last update was just 500ms ago
        long lastUpdateEpoch = Instant.now().minusMillis(500).toEpochMilli();
        when(valueOperations.get("voum:trip:tracking:last_update:" + tripId)).thenReturn(String.valueOf(lastUpdateEpoch));

        LocationUpdateRequest req = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                trackingService.updateLocation(tripId, driverId, req)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    public void testUpdateLocation_writeReductionLogic_saveToDBOnlyOnThrottlingRules() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .motariId(driverId)
                .status("MOTARI_EN_ROUTE")
                .build();

        UserLocation location = UserLocation.builder()
                .userId(driverId)
                .availabilityStatus("BUSY")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userLocationRepository.findByUserId(driverId)).thenReturn(Optional.of(location));
        
        // No last update (rate limit ok)
        when(valueOperations.get("voum:trip:tracking:last_update:" + tripId)).thenReturn(null);

        // Sequence number setup
        when(valueOperations.increment("voum:trip:tracking:seq:" + tripId)).thenReturn(1L);

        // Scenario 1: First update ever. No last saved exists in Redis/DB. Should save to DB.
        when(valueOperations.get("voum:trip:tracking:last_saved:" + tripId)).thenReturn(null);
        when(tripTrackingPointRepository.findTop100ByTripIdOrderBySequenceNumberDesc(tripId)).thenReturn(Collections.emptyList());

        LocationUpdateRequest req1 = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        TripLocationResponse res1 = trackingService.updateLocation(tripId, driverId, req1);
        
        assertNotNull(res1);
        verify(tripTrackingPointRepository, times(1)).save(any(TripTrackingPoint.class));

        // Reset verification counts
        clearInvocations(tripTrackingPointRepository);

        // Scenario 2: Second update, moved only 2 meters and 5 seconds elapsed. Should NOT save to DB.
        // We simulate caching the first update as "last_saved" in Redis.
        TripLocationResponse lastSaved = TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(-1.9441)
                .longitude(30.0619)
                .recordedAt(Instant.now().minusSeconds(5))
                .build();
        when(valueOperations.get("voum:trip:tracking:last_saved:" + tripId)).thenReturn(objectMapper.writeValueAsString(lastSaved));

        LocationUpdateRequest req2 = LocationUpdateRequest.builder()
                .latitude(-1.94412) // roughly 2 meters away
                .longitude(30.0619)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        TripLocationResponse res2 = trackingService.updateLocation(tripId, driverId, req2);
        assertNotNull(res2);
        verify(tripTrackingPointRepository, never()).save(any(TripTrackingPoint.class));

        // Scenario 3: Third update, moved 15 meters (approx 0.015 km). Should save to DB.
        LocationUpdateRequest req3 = LocationUpdateRequest.builder()
                .latitude(-1.9443) // roughly ~22 meters away
                .longitude(30.0620)
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        TripLocationResponse res3 = trackingService.updateLocation(tripId, driverId, req3);
        assertNotNull(res3);
        verify(tripTrackingPointRepository, times(1)).save(any(TripTrackingPoint.class));

        // Reset verifications
        clearInvocations(tripTrackingPointRepository);

        // Scenario 4: Fourth update, moved only 1 meter but 20 seconds elapsed. Should save to DB.
        TripLocationResponse lastSaved2 = TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(-1.9443)
                .longitude(30.0620)
                .recordedAt(Instant.now().minusSeconds(20))
                .build();
        when(valueOperations.get("voum:trip:tracking:last_saved:" + tripId)).thenReturn(objectMapper.writeValueAsString(lastSaved2));

        LocationUpdateRequest req4 = LocationUpdateRequest.builder()
                .latitude(-1.9443)
                .longitude(30.06201) // 1 meter away
                .accuracy(5.0)
                .speedKmh(30.0)
                .headingDegrees(120.0)
                .build();

        TripLocationResponse res4 = trackingService.updateLocation(tripId, driverId, req4);
        assertNotNull(res4);
        verify(tripTrackingPointRepository, times(1)).save(any(TripTrackingPoint.class));

        // Ensure real-time WebSocket event was fired on every update
        verify(eventPublisher, times(4)).publishEvent(any(TripLocationUpdatedEvent.class));
    }

    @Test
    public void testGetEta_clampingBounds() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(userId)
                .motariId(UUID.randomUUID())
                .pickupLatitude(-1.9441)
                .pickupLongitude(30.0619)
                .status("MOTARI_EN_ROUTE")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(valueOperations.get("voum:trip:eta:" + tripId)).thenReturn(null);

        // Scenario A: Driver is 100 meters away, moving at 40km/h (ETA calculated < 1 min). Clamps to 1.
        TripLocationResponse latestClose = TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(-1.94415) // ~5 meters away
                .longitude(30.0619)
                .speedKmh(40.0)
                .build();
        when(valueOperations.get("voum:trip:tracking:latest:" + tripId)).thenReturn(objectMapper.writeValueAsString(latestClose));

        TripEtaResponse eta1 = trackingService.getEta(tripId, userId);
        assertEquals(1, eta1.getEstimatedArrivalMinutes());

        // Scenario B: Driver is 150 km away. Clamps to max 120 mins.
        TripLocationResponse latestFar = TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(-1.0) // ~100+ km away
                .longitude(30.0)
                .speedKmh(30.0)
                .build();
        when(valueOperations.get("voum:trip:tracking:latest:" + tripId)).thenReturn(objectMapper.writeValueAsString(latestFar));

        TripEtaResponse eta2 = trackingService.getEta(tripId, userId);
        assertEquals(120, eta2.getEstimatedArrivalMinutes());
    }

    @Test
    public void testArrivalProximityUtilities() {
        Trip trip = Trip.builder()
                .pickupLatitude(-1.9441)
                .pickupLongitude(30.0619)
                .destinationLatitude(-1.9584)
                .destinationLongitude(30.0875)
                .build();

        // 10 meters away from pickup -> should detect near pickup
        assertTrue(trackingService.isNearPickup(trip, -1.94415, 30.0619));

        // 100 meters away from pickup -> should NOT detect near pickup
        assertFalse(trackingService.isNearPickup(trip, -1.9450, 30.0619));

        // 15 meters away from destination -> should detect near destination
        assertTrue(trackingService.isNearDestination(trip, -1.9584, 30.0876));
    }
}
