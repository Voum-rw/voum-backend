package com.voum.modules.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.configuration.JwtTokenProvider;
import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.*;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.realtime.dto.*;
import com.voum.modules.realtime.listeners.MarketplaceDomainEventListener;
import com.voum.modules.realtime.listeners.RedisMessageSubscriber;
import com.voum.modules.realtime.publishers.RedisMessagePublisher;
import com.voum.modules.realtime.websocket.SubscriptionAuthInterceptor;
import com.voum.modules.trip.dto.TripResponse;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.events.TripCreatedEvent;
import com.voum.modules.trip.mapper.TripMapper;
import com.voum.modules.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RealtimeBroadcastingTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private RedisMessagePublisher redisPublisher;

    @Mock
    private LocationService locationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private TripMapper tripMapper;

    private SubscriptionAuthInterceptor authInterceptor;
    private MarketplaceDomainEventListener domainEventListener;
    private RedisMessageSubscriber messageSubscriber;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        authInterceptor = new SubscriptionAuthInterceptor(tokenProvider, rideRequestRepository, tripRepository);
        domainEventListener = new MarketplaceDomainEventListener(redisPublisher, locationService, rideRequestRepository, tripMapper);
        objectMapper = new ObjectMapper();
        messageSubscriber = new RedisMessageSubscriber(messagingTemplate, objectMapper);
    }

    // ==========================================
    // 1. SubscriptionAuthInterceptor Tests
    // ==========================================

    @Test
    public void testConnect_withValidToken_shouldAuthenticate() {
        UUID userId = UUID.randomUUID();
        String mockToken = "valid-token";
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + mockToken);
        
        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        
        when(tokenProvider.validateToken(mockToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(mockToken)).thenReturn(userId);
        when(tokenProvider.getRoleFromToken(mockToken)).thenReturn("MOTARI");

        org.springframework.messaging.Message<?> result = authInterceptor.preSend(message, mock(MessageChannel.class));
        assertNotNull(result);
        
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        Principal principal = resultAccessor.getUser();
        assertNotNull(principal);
        assertEquals(userId.toString(), principal.getName());
    }

    @Test
    public void testConnect_withInvalidToken_shouldThrowException() {
        String mockToken = "invalid-token";
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + mockToken);
        
        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        
        when(tokenProvider.validateToken(mockToken)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            authInterceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    public void testSubscribe_ownDriverChannel_shouldSucceed() {
        UUID driverId = UUID.randomUUID();
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/driver/" + driverId);
        
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(driverId, null);
        accessor.setUser(principal);
        
        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        
        org.springframework.messaging.Message<?> result = authInterceptor.preSend(message, mock(MessageChannel.class));
        assertNotNull(result);
    }

    @Test
    public void testSubscribe_otherDriverChannel_shouldThrowException() {
        UUID driverId = UUID.randomUUID();
        UUID otherDriverId = UUID.randomUUID();
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/driver/" + otherDriverId);
        
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(driverId, null);
        accessor.setUser(principal);
        
        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        
        assertThrows(IllegalArgumentException.class, () -> 
            authInterceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    public void testSubscribe_ownPassengerRequestChannel_shouldSucceed() {
        UUID passengerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/request/" + requestId);
        
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(passengerId, null);
        accessor.setUser(principal);
        
        RideRequest mockRequest = RideRequest.builder()
                .id(requestId)
                .passengerId(passengerId)
                .build();
        
        when(rideRequestRepository.findById(requestId)).thenReturn(Optional.of(mockRequest));

        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        org.springframework.messaging.Message<?> result = authInterceptor.preSend(message, mock(MessageChannel.class));
        assertNotNull(result);
    }

    @Test
    public void testSubscribe_otherPassengerRequestChannel_shouldThrowException() {
        UUID passengerId = UUID.randomUUID();
        UUID otherPassengerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/request/" + requestId);
        
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(passengerId, null);
        accessor.setUser(principal);
        
        RideRequest mockRequest = RideRequest.builder()
                .id(requestId)
                .passengerId(otherPassengerId) // owned by someone else
                .build();
        
        when(rideRequestRepository.findById(requestId)).thenReturn(Optional.of(mockRequest));

        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        
        assertThrows(IllegalArgumentException.class, () -> 
            authInterceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    public void testSubscribe_ownTripChannel_shouldSucceed() {
        UUID passengerId = UUID.randomUUID();
        UUID motariId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/trip/" + tripId);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(passengerId, null);
        accessor.setUser(principal);

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(motariId)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        org.springframework.messaging.Message<?> result = authInterceptor.preSend(message, mock(MessageChannel.class));
        assertNotNull(result);
    }

    @Test
    public void testSubscribe_otherTripChannel_shouldThrowException() {
        UUID otherUserId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        UUID motariId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/trip/" + tripId);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(otherUserId, null);
        accessor.setUser(principal);

        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(passengerId)
                .motariId(motariId)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        org.springframework.messaging.Message<?> message = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        
        assertThrows(IllegalArgumentException.class, () -> 
            authInterceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    // ==========================================
    // 2. MarketplaceDomainEventListener Tests
    // ==========================================

    @Test
    public void testHandleRequestCreated_shouldFindDriversAndPublish() {
        UUID requestId = UUID.randomUUID();
        RideRequest request = RideRequest.builder()
                .id(requestId)
                .pickupLatitude(-1.9441)
                .pickupLongitude(30.0619)
                .visibilityRadiusKm(3.0)
                .status("OPEN")
                .build();
        
        RideRequestCreatedEvent event = new RideRequestCreatedEvent(this, request);

        UUID driverId1 = UUID.randomUUID();
        UUID driverId2 = UUID.randomUUID();
        
        NearbyMotariResponse nearbyDriver1 = new NearbyMotariResponse();
        nearbyDriver1.setMotariId(driverId1);
        NearbyMotariResponse nearbyDriver2 = new NearbyMotariResponse();
        nearbyDriver2.setMotariId(driverId2);
        
        when(locationService.findNearbyMotaris(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Arrays.asList(nearbyDriver1, nearbyDriver2));

        domainEventListener.handleRequestCreated(event);

        verify(redisPublisher, times(1)).publish(
                eq("REQUEST_CREATED"),
                isNull(),
                isNull(),
                eq(Arrays.asList(driverId1, driverId2)),
                any(RequestCreatedMessage.class)
        );
    }

    @Test
    public void testHandleOfferSubmitted_shouldPublishNewOffer() {
        UUID requestId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID motariId = UUID.randomUUID();

        RideOffer offer = RideOffer.builder()
                .id(offerId)
                .rideRequestId(requestId)
                .motariId(motariId)
                .offeredPrice(2200.0)
                .estimatedArrivalMinutes(3)
                .status("PENDING")
                .build();

        RideRequest request = RideRequest.builder()
                .id(requestId)
                .offersCount(1)
                .build();

        when(rideRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        RideOfferSubmittedEvent event = new RideOfferSubmittedEvent(this, offer);
        domainEventListener.handleOfferSubmitted(event);

        verify(redisPublisher, times(1)).publish(
                eq("NEW_OFFER"),
                eq(requestId),
                isNull(),
                isNull(),
                any(OfferCreatedMessage.class)
        );
    }

    @Test
    public void testHandleOfferAccepted_shouldPublishOfferAccepted() {
        UUID requestId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        
        RideRequest request = RideRequest.builder().id(requestId).status("MATCHED").build();
        RideOffer offer = RideOffer.builder().id(offerId).rideRequestId(requestId).status("ACCEPTED").build();

        RideOfferAcceptedEvent event = new RideOfferAcceptedEvent(this, request, offer);
        domainEventListener.handleOfferAccepted(event);

        verify(redisPublisher, times(1)).publish(
                eq("OFFER_ACCEPTED"),
                eq(requestId),
                isNull(),
                isNull(),
                any(OfferAcceptedMessage.class)
        );
    }

    @Test
    public void testHandleTripCreatedEvent_shouldPublishToRedis() {
        UUID tripId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .id(tripId)
                .passengerId(UUID.randomUUID())
                .motariId(UUID.randomUUID())
                .agreedPrice(2200.0)
                .status("CREATED")
                .build();

        TripCreatedEvent event = new TripCreatedEvent(this, trip);
        domainEventListener.handleTripCreated(event);

        verify(redisPublisher, times(1)).publish(
                eq("TRIP_CREATED"),
                isNull(),
                eq(tripId),
                isNull(),
                any(TripResponse.class)
        );
    }

    // ==========================================
    // 3. RedisMessageSubscriber Tests
    // ==========================================

    @Test
    public void testRedisMessageSubscriber_shouldBroadcastToDriversAndPassengersAndTrips() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        
        OfferCreatedMessage payload = OfferCreatedMessage.builder()
                .eventType("NEW_OFFER")
                .offerId(UUID.randomUUID())
                .price(2200.0)
                .build();

        RealtimeEventMessage eventMessage = RealtimeEventMessage.builder()
                .eventType("NEW_OFFER")
                .eventVersion(1)
                .sequence(10L)
                .driverIds(Collections.singletonList(driverId))
                .requestId(requestId)
                .tripId(tripId)
                .payload(payload)
                .build();

        byte[] serialized = objectMapper.writeValueAsBytes(eventMessage);
        
        Message redisMessage = mock(Message.class);
        when(redisMessage.getBody()).thenReturn(serialized);

        messageSubscriber.onMessage(redisMessage, new byte[0]);

        // Verify driver WebSocket broadcast
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/driver/" + driverId),
                any(Object.class)
        );

        // Verify passenger WebSocket broadcast
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/request/" + requestId),
                any(Object.class)
        );

        // Verify trip WebSocket broadcast
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/trip/" + tripId),
                any(Object.class)
        );
    }
}
