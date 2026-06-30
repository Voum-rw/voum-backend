package com.voum.modules.marketplace;

import com.voum.common.ApiException;
import com.voum.modules.marketplace.dto.RideRequestCreateRequest;
import com.voum.modules.marketplace.dto.RideRequestResponse;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.RideRequestCreatedEvent;
import com.voum.modules.marketplace.events.RideRequestExpiredEvent;
import com.voum.modules.marketplace.mapper.MarketplaceMapper;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.marketplace.service.RideRequestService;
import com.voum.modules.users.PassengerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideRequestServiceTest {

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private MarketplaceMapper marketplaceMapper;

    @InjectMocks
    private RideRequestService rideRequestService;

    @Test
    void createRequest_shouldSucceedAndPublishEvent() {
        UUID passengerId = UUID.randomUUID();
        RideRequestCreateRequest req = RideRequestCreateRequest.builder()
                .pickupLatitude(-1.9441)
                .pickupLongitude(30.0619)
                .destinationLatitude(-1.9584)
                .destinationLongitude(30.0875)
                .pickupAddress("Kimironko")
                .destinationAddress("Kigali Heights")
                .proposedBudget(2500.0)
                .visibilityRadiusKm(3.0)
                .build();

        when(passengerRepository.existsById(passengerId)).thenReturn(true);
        when(rideRequestRepository.existsByPassengerIdAndStatus(passengerId, "OPEN")).thenReturn(false);
        when(rideRequestRepository.save(any(RideRequest.class))).thenAnswer(invocation -> {
            RideRequest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RideRequestResponse response = rideRequestService.createRequest(passengerId, req);

        assertNotNull(response);
        assertEquals("OPEN", response.getStatus());
        assertEquals(2500.0, response.getProposedBudget());
        verify(eventPublisher, times(1)).publishEvent(any(RideRequestCreatedEvent.class));
    }

    @Test
    void createRequest_withAnotherActiveOpenRequest_shouldThrowException() {
        UUID passengerId = UUID.randomUUID();
        RideRequestCreateRequest req = RideRequestCreateRequest.builder()
                .pickupLatitude(-1.9441)
                .pickupLongitude(30.0619)
                .destinationLatitude(-1.9584)
                .destinationLongitude(30.0875)
                .pickupAddress("Kimironko")
                .destinationAddress("Kigali Heights")
                .proposedBudget(2500.0)
                .build();

        when(passengerRepository.existsById(passengerId)).thenReturn(true);
        // Passenger already has an active request
        when(rideRequestRepository.existsByPassengerIdAndStatus(passengerId, "OPEN")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                rideRequestService.createRequest(passengerId, req));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("already have an active open"));
    }

    @Test
    void cleanExpiredRequests_shouldUpdateStatusAndPublishEvents() {
        UUID reqId = UUID.randomUUID();
        RideRequest request = RideRequest.builder()
                .id(reqId)
                .status("OPEN")
                .expiresAt(Instant.now().minus(10, ChronoUnit.SECONDS))
                .build();

        when(rideRequestRepository.findByStatusAndExpiresAtBefore(eq("OPEN"), any(Instant.class)))
                .thenReturn(Collections.singletonList(request));

        rideRequestService.cleanExpiredRequests();

        assertEquals("EXPIRED", request.getStatus());
        verify(rideRequestRepository, times(1)).save(request);
        verify(eventPublisher, times(1)).publishEvent(any(RideRequestExpiredEvent.class));
    }
}
