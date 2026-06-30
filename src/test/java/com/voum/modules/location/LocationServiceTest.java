package com.voum.modules.location;

import com.voum.common.ApiException;
import com.voum.modules.location.dto.LocationUpdateRequest;
import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.mapper.LocationMapper;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.service.LocationCacheService;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.location.util.HaversineCalculator;
import com.voum.modules.users.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MotariRepository motariRepository;

    @Mock
    private LocationCacheService cacheService;

    @Spy
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    @Test
    void testHaversineDistance_betweenKigaliPoints_shouldBeCorrect() {
        // Coordinates for Kigali Center and Kigali Heights (~3.2 km apart)
        double kigaliCenterLat = -1.9441;
        double kigaliCenterLng = 30.0619;
        double kigaliHeightsLat = -1.9584;
        double kigaliHeightsLng = 30.0875;

        double distance = HaversineCalculator.calculateDistance(
                kigaliCenterLat, kigaliCenterLng,
                kigaliHeightsLat, kigaliHeightsLng
        );

        // Distance should be approximately 3.23 km
        assertEquals(3.23, distance, 0.1);
    }

    @Test
    void goOnline_withUnapprovedDriver_shouldThrowException() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role(Role.MOTARI).build();
        Motari motari = Motari.builder()
                .id(userId)
                .onboardingStatus("IN_PROGRESS")
                .verificationStatus("PENDING")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(motariRepository.findById(userId)).thenReturn(Optional.of(motari));

        LocationUpdateRequest req = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(5.0)
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                locationService.goOnline(userId, req));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getMessage().contains("Only approved and completed Motaris"));
    }

    @Test
    void goOnline_withApprovedDriver_shouldUpsertLocationToOnline() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role(Role.MOTARI).build();
        Motari motari = Motari.builder()
                .id(userId)
                .onboardingStatus("COMPLETED")
                .verificationStatus("APPROVED")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(motariRepository.findById(userId)).thenReturn(Optional.of(motari));
        when(userLocationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        LocationUpdateRequest req = LocationUpdateRequest.builder()
                .latitude(-1.9441)
                .longitude(30.0619)
                .accuracy(3.0)
                .build();

        locationService.goOnline(userId, req);

        verify(userLocationRepository, times(1)).save(argThat(loc ->
                "ONLINE".equals(loc.getAvailabilityStatus()) &&
                userId.equals(loc.getUserId()) &&
                Double.valueOf(-1.9441).equals(loc.getLatitude())
        ));
        verify(cacheService, times(1)).cacheOnlineMotari(userId);
    }

    @Test
    void findNearbyMotaris_shouldApplyBoundingBoxAndHaversineFilter() {
        double centerLat = -1.9441;
        double centerLng = 30.0619;
        double radius = 5.0; // 5 km

        UUID closeDriverId = UUID.randomUUID();
        UUID farDriverId = UUID.randomUUID(); // Outside 5km but potentially inside bounding box

        UserLocation closeLoc = UserLocation.builder()
                .userId(closeDriverId)
                .latitude(-1.9450)
                .longitude(30.0620)
                .availabilityStatus("ONLINE")
                .lastSeenAt(Instant.now())
                .build();

        UserLocation farLoc = UserLocation.builder()
                .userId(farDriverId)
                .latitude(-1.9999) // ~7.5 km away
                .longitude(30.0999)
                .availabilityStatus("ONLINE")
                .lastSeenAt(Instant.now())
                .build();

        Motari closeMotari = Motari.builder()
                .id(closeDriverId)
                .firstName("John")
                .verificationStatus("APPROVED")
                .build();

        Motari farMotari = Motari.builder()
                .id(farDriverId)
                .firstName("Adam")
                .verificationStatus("APPROVED")
                .build();

        when(userLocationRepository.findDriversInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(Instant.class)))
                .thenReturn(Arrays.asList(closeLoc, farLoc));
        when(motariRepository.findAllById(anyList())).thenReturn(Arrays.asList(closeMotari, farMotari));

        List<NearbyMotariResponse> results = locationService.findNearbyMotaris(centerLat, centerLng, radius);

        assertNotNull(results);
        assertEquals(1, results.size()); // Far driver is filtered out by precise Haversine distance
        assertEquals(closeDriverId, results.get(0).getMotariId());
        assertEquals("John", results.get(0).getFirstName());
    }

    @Test
    void cleanStaleSessions_shouldTransitionInactiveDriversOffline() {
        UUID staleDriverId = UUID.randomUUID();
        UserLocation staleLoc = UserLocation.builder()
                .userId(staleDriverId)
                .availabilityStatus("ONLINE")
                .lastSeenAt(Instant.now().minus(10, ChronoUnit.MINUTES)) // 10 mins ago (stale)
                .build();

        when(userLocationRepository.findAll()).thenReturn(Arrays.asList(staleLoc));

        locationService.cleanStaleSessions();

        verify(cacheService, times(1)).evictOnlineMotari(staleDriverId);
        verify(userLocationRepository, times(1)).markStaleUsersOffline(any(Instant.class));
    }
}
