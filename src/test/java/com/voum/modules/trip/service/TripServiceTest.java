package com.voum.modules.trip.service;

import com.voum.common.ApiException;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.RideOfferAcceptedEvent;
import com.voum.modules.trip.dto.TripResponse;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.events.*;
import com.voum.modules.trip.mapper.TripMapper;
import com.voum.modules.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private TripMapper tripMapper;

    private TripService tripService;

    @BeforeEach
    public void setup() {
        tripService = new TripService(tripRepository, userLocationRepository, locationService, tripMapper, eventPublisher);
    }

    @Test
    public void testHandleRideOfferAccepted_shouldCreateTripInCreatedStatus() {
        UUID passengerId = UUID.randomUUID();
        UUID motariId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        RideRequest request = RideRequest.builder()
                .id(requestId)
                .passengerId(passengerId)
                .pickupLatitude(-1.9441)
                .pickupLongitude(30.0619)
                .pickupAddress("Kigali Town")
                .destinationLatitude(-1.9584)
                .destinationLongitude(30.0875)
                .destinationAddress("Gikondo")
                .build();

        RideOffer offer = RideOffer.builder()
                .id(offerId)
                .rideRequestId(requestId)
                .motariId(motariId)
                .offeredPrice(2200.0)
                .estimatedArrivalMinutes(5)
                .build();

        UserLocation driverLocation = UserLocation.builder()
                .userId(motariId)
                .latitude(-1.9450)
                .longitude(30.0620)
                .build();

        when(userLocationRepository.findByUserId(motariId)).thenReturn(Optional.of(driverLocation));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        RideOfferAcceptedEvent event = new RideOfferAcceptedEvent(this, request, offer);
        tripService.handleRideOfferAccepted(event);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        Trip savedTrip = tripCaptor.getValue();

        assertNotNull(savedTrip);
        assertEquals(requestId, savedTrip.getRideRequestId());
        assertEquals(offerId, savedTrip.getRideOfferId());
        assertEquals(passengerId, savedTrip.getPassengerId());
        assertEquals(motariId, savedTrip.getMotariId());
        assertEquals("CREATED", savedTrip.getStatus());
        assertEquals(2200.0, savedTrip.getAgreedPrice());
        assertEquals(5, savedTrip.getEstimatedArrivalMinutes());
        assertTrue(savedTrip.getEstimatedDistanceKm() > 0.0);

        verify(eventPublisher, times(1)).publishEvent(any(TripCreatedEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(TripStatusChangedEvent.class));
    }

    @Test
    public void testMarkEnRoute_byDriver_shouldTransitionStatus() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("CREATED")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.markEnRoute(tripId, driverId);

        assertNotNull(response);
        assertEquals("MOTARI_EN_ROUTE", response.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(MotariEnRouteEvent.class));
    }

    @Test
    public void testMarkEnRoute_byPassenger_shouldThrowForbidden() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("CREATED")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        ApiException ex = assertThrows(ApiException.class, () ->
            tripService.markEnRoute(tripId, passengerId)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    public void testMarkArrived_fromEnRoute_shouldTransitionStatus() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("MOTARI_EN_ROUTE")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.markArrived(tripId, driverId);

        assertNotNull(response);
        assertEquals("MOTARI_ARRIVED", response.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(MotariArrivedEvent.class));
    }

    @Test
    public void testMarkBoarded_byPassenger_shouldTransitionStatus() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("MOTARI_ARRIVED")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.markBoarded(tripId, passengerId);

        assertNotNull(response);
        assertEquals("PASSENGER_ONBOARD", response.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(PassengerBoardedEvent.class));
    }

    @Test
    public void testStartTrip_byDriver_shouldTransitionToInProgress() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("PASSENGER_ONBOARD")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.startTrip(tripId, driverId);

        assertNotNull(response);
        assertEquals("IN_PROGRESS", response.getStatus());
        assertNotNull(response.getStartedAt());
        verify(eventPublisher, times(1)).publishEvent(any(TripStartedEvent.class));
    }

    @Test
    public void testCompleteTrip_byDriver_shouldTransitionToCompletedAndResetDriver() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("IN_PROGRESS")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.completeTrip(tripId, driverId);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getCompletedAt());
        verify(locationService, times(1)).updateAvailabilityStatus(driverId, "ONLINE");
        verify(eventPublisher, times(1)).publishEvent(any(TripCompletedEvent.class));
    }

    @Test
    public void testCancelTrip_byPassenger_shouldSucceedWhenCreated() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("CREATED")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.cancelTrip(tripId, passengerId, "Changed my mind");

        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        assertEquals("Changed my mind", response.getCancellationReason());
        assertEquals(passengerId, response.getCancelledBy());
        assertNotNull(response.getCancelledAt());
        
        verify(locationService, times(1)).updateAvailabilityStatus(driverId, "ONLINE");
        verify(eventPublisher, times(1)).publishEvent(any(TripCancelledEvent.class));
    }

    @Test
    public void testCancelTrip_whenInProgress_shouldThrowBadRequest() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("IN_PROGRESS")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        ApiException ex = assertThrows(ApiException.class, () ->
            tripService.cancelTrip(tripId, passengerId, "Cancel please")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void testIllegalTransition_shouldThrowBadRequest() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(driverId)
                .status("CREATED")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        // CREATED -> COMPLETED is illegal
        ApiException ex = assertThrows(ApiException.class, () ->
            tripService.completeTrip(tripId, driverId)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
