package com.voum.modules.realtime.websocket;

import com.voum.configuration.JwtTokenProvider;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAuthInterceptor.class);

    private final JwtTokenProvider tokenProvider;
    private final RideRequestRepository rideRequestRepository;
    @org.springframework.context.annotation.Lazy
    private final com.voum.modules.trip.repository.TripRepository tripRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (tokenProvider.validateToken(token)) {
                    UUID userId = tokenProvider.getUserIdFromToken(token);
                    String role = tokenProvider.getRoleFromToken(token);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.singletonList(authority));
                    
                    // Create a mutable copy of the accessor if it is not currently mutable
                    StompHeaderAccessor mutableAccessor = accessor.isMutable() ? accessor : StompHeaderAccessor.wrap(message);
                    mutableAccessor.setUser(auth);
                    
                    log.info("WebSocket CONNECT authenticated for user: {} with role: {}", userId, role);
                    
                    // Rebuild the message with the updated authenticated principal
                    return org.springframework.messaging.support.MessageBuilder.createMessage(
                            message.getPayload(), mutableAccessor.getMessageHeaders());
                } else {
                    log.warn("WebSocket CONNECT rejected: Invalid JWT token");
                    throw new IllegalArgumentException("Access Denied: Invalid JWT token");
                }
            } else {
                log.warn("WebSocket CONNECT rejected: Missing Authorization header");
                throw new IllegalArgumentException("Access Denied: Missing Authorization header");
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (dest == null) {
                return message;
            }

            if (principal == null) {
                log.warn("WebSocket SUBSCRIBE rejected: Unauthenticated user subscribing to {}", dest);
                throw new IllegalArgumentException("Access Denied: Unauthenticated subscription request.");
            }

            UUID userId = UUID.fromString(principal.getName());

            if (dest.startsWith("/topic/driver/")) {
                String driverIdStr = dest.substring("/topic/driver/".length());
                try {
                    UUID driverId = UUID.fromString(driverIdStr);
                    if (!userId.equals(driverId)) {
                        log.warn("WebSocket SUBSCRIBE rejected: User {} attempted to subscribe to driver channel {}", userId, driverId);
                        throw new IllegalArgumentException("Access Denied: You are not authorized to subscribe to this driver channel.");
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Access Denied: Invalid driver ID format.");
                }
            } else if (dest.startsWith("/topic/request/")) {
                String requestIdStr = dest.substring("/topic/request/".length());
                try {
                    UUID requestId = UUID.fromString(requestIdStr);
                    RideRequest request = rideRequestRepository.findById(requestId).orElse(null);
                    if (request == null) {
                        log.warn("WebSocket SUBSCRIBE rejected: Request {} not found", requestId);
                        throw new IllegalArgumentException("Access Denied: Ride request not found.");
                    }
                    if (!request.getPassengerId().equals(userId)) {
                        log.warn("WebSocket SUBSCRIBE rejected: User {} attempted to subscribe to request channel {} owned by passenger {}", 
                                userId, requestId, request.getPassengerId());
                        throw new IllegalArgumentException("Access Denied: You are not authorized to subscribe to this request.");
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Access Denied: Invalid request ID format.");
                }
            } else if (dest.startsWith("/topic/trip/")) {
                String tripIdStr = dest.substring("/topic/trip/".length());
                try {
                    UUID tripId = UUID.fromString(tripIdStr);
                    com.voum.modules.trip.entity.Trip trip = tripRepository.findById(tripId).orElse(null);
                    if (trip == null) {
                        log.warn("WebSocket SUBSCRIBE rejected: Trip {} not found", tripId);
                        throw new IllegalArgumentException("Access Denied: Trip not found.");
                    }
                    if (!trip.getPassengerId().equals(userId) && !trip.getMotariId().equals(userId)) {
                        log.warn("WebSocket SUBSCRIBE rejected: User {} is not assigned to trip {}", userId, tripId);
                        throw new IllegalArgumentException("Access Denied: You are not authorized to subscribe to this trip.");
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Access Denied: Invalid trip ID format.");
                }
            } else if (dest.startsWith("/topic/chat/")) {
                String contextIdStr = dest.substring("/topic/chat/".length());
                try {
                    UUID contextId = UUID.fromString(contextIdStr);
                    // Try to find as RideRequest first
                    RideRequest request = rideRequestRepository.findById(contextId).orElse(null);
                    if (request != null) {
                        boolean isPassenger = request.getPassengerId().equals(userId);
                        boolean isMotari = false;
                        if (principal instanceof org.springframework.security.core.Authentication) {
                            isMotari = ((org.springframework.security.core.Authentication) principal).getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_MOTARI"));
                        }
                        if (!isPassenger && !isMotari) {
                            log.warn("WebSocket SUBSCRIBE rejected: User {} not authorized to chat for request {}", userId, contextId);
                            throw new IllegalArgumentException("Access Denied: You are not authorized to join this request's chat.");
                        }
                    } else {
                        // Try to find as Trip
                        com.voum.modules.trip.entity.Trip trip = tripRepository.findById(contextId).orElse(null);
                        if (trip != null) {
                            if (!trip.getPassengerId().equals(userId) && !trip.getMotariId().equals(userId)) {
                                log.warn("WebSocket SUBSCRIBE rejected: User {} not authorized to chat for trip {}", userId, contextId);
                                throw new IllegalArgumentException("Access Denied: You are not authorized to join this trip's chat.");
                            }
                        } else {
                            log.warn("WebSocket SUBSCRIBE rejected: Chat context {} not found", contextId);
                            throw new IllegalArgumentException("Access Denied: Conversation context not found.");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Access Denied: Invalid context ID format.");
                }
            }
        }
        return message;
    }
}
