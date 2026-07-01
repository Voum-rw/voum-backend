package com.voum.modules.realtime.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.modules.realtime.dto.RealtimeEventMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            RealtimeEventMessage event = objectMapper.readValue(body, RealtimeEventMessage.class);

            log.debug("Received realtime event from Redis: {} (sequence={})", event.getEventType(), event.getSequence());

            // 1. Broadcast to matching drivers (Motaris)
            if (event.getDriverIds() != null && !event.getDriverIds().isEmpty()) {
                for (UUID driverId : event.getDriverIds()) {
                    String destination = "/topic/driver/" + driverId;
                    messagingTemplate.convertAndSend(destination, event.getPayload());
                    log.debug("Forwarding payload to driver topic: {}", destination);
                }
            }

            // 2. Broadcast to passenger request channel
            if (event.getRequestId() != null) {
                String destination = "/topic/request/" + event.getRequestId();
                messagingTemplate.convertAndSend(destination, event.getPayload());
                log.debug("Forwarding payload to request topic: {}", destination);
            }

            // 3. Broadcast to trip channel
            if (event.getTripId() != null) {
                String destination = "/topic/trip/" + event.getTripId();
                messagingTemplate.convertAndSend(destination, event.getPayload());
                log.debug("Forwarding payload to trip topic: {}", destination);
            }

        } catch (IOException e) {
            log.error("Failed to deserialize realtime event message from Redis", e);
        }
    }
}
