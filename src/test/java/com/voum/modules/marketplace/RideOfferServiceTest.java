package com.voum.modules.marketplace;

import com.voum.common.ApiException;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.service.LocationService;
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
import com.voum.modules.marketplace.service.RideOfferService;
import com.voum.modules.users.*;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideOfferServiceTest {

    @Mock
    private RideOfferRepository rideOfferRepository;

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private MotariRepository motariRepository;

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private MarketplaceMapper marketplaceMapper;

    @InjectMocks
    private RideOfferService rideOfferService;

    @Test
    void submitOffer_shouldSucceedWhenMotariIsOnlineAndApproved() {
        UUID motariId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        Motari motari = Motari.builder().id(motariId).verificationStatus("APPROVED").build();
        UserLocation location = UserLocation.builder().userId(motariId).availabilityStatus("ONLINE").latitude(-1.9441).longitude(30.0619).build();
        RideRequest request = RideRequest.builder().id(requestId).status("OPEN").expiresAt(Instant.now().plus(60, ChronoUnit.SECONDS)).pickupLatitude(-1.9441).pickupLongitude(30.0619).build();

        when(motariRepository.findById(motariId)).thenReturn(Optional.of(motari));
        when(userLocationRepository.findByUserId(motariId)).thenReturn(Optional.of(location));
        when(rideRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(rideOfferRepository.existsByRideRequestIdAndMotariIdAndStatus(requestId, motariId, "PENDING")).thenReturn(false);
        when(rideOfferRepository.save(any(RideOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideOfferCreateRequest req = RideOfferCreateRequest.builder()
                .rideRequestId(requestId)
                .offeredPrice(2200.0)
                .estimatedArrivalMinutes(3)
                .build();

        RideOfferResponse response = rideOfferService.submitOffer(motariId, req);

        assertNotNull(response);
        assertEquals(2200.0, response.getOfferedPrice());
        assertEquals("PENDING", response.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(RideOfferSubmittedEvent.class));
    }

    @Test
    void updateOffer_exceedingMaxUpdates_shouldThrowException() {
        UUID motariId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        RideOffer offer = RideOffer.builder()
                .id(offerId)
                .rideRequestId(requestId)
                .motariId(motariId)
                .status("PENDING")
                .updateCount(5) // Max updates reached
                .build();

        RideRequest request = RideRequest.builder()
                .id(requestId)
                .status("OPEN")
                .expiresAt(Instant.now().plus(60, ChronoUnit.SECONDS))
                .build();

        when(rideOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(rideRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        RideOfferUpdateRequest req = RideOfferUpdateRequest.builder()
                .offeredPrice(2300.0)
                .estimatedArrivalMinutes(4)
                .build();

        ApiException exception = assertThrows(ApiException.class, () ->
                rideOfferService.updateOffer(motariId, offerId, req));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Maximum price updates limit"));
    }

    @Test
    void acceptOffer_shouldSetStatusToMatchedAndDriverBusy() {
        UUID passengerId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID motariId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        RideOffer offer = RideOffer.builder().id(offerId).rideRequestId(requestId).motariId(motariId).status("PENDING").build();
        RideRequest request = RideRequest.builder().id(requestId).passengerId(passengerId).status("OPEN").expiresAt(Instant.now().plus(60, ChronoUnit.SECONDS)).build();

        when(rideOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(rideRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(rideOfferRepository.findByRideRequestIdAndStatus(requestId, "PENDING")).thenReturn(Collections.singletonList(offer));

        rideOfferService.acceptOffer(passengerId, offerId);

        assertEquals("ACCEPTED", offer.getStatus());
        assertEquals("MATCHED", request.getStatus());
        assertEquals(offerId, request.getSelectedOfferId());
        
        verify(locationService, times(1)).updateAvailabilityStatus(motariId, "BUSY");
        verify(eventPublisher, times(1)).publishEvent(any(RideOfferAcceptedEvent.class));
    }
}
