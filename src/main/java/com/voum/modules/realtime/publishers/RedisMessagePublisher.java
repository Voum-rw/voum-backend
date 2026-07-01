package com.voum.modules.realtime.publishers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.modules.realtime.config.RedisRealtimeConfig;
import com.voum.modules.realtime.dto.RealtimeEventMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisMessagePublisher.class);
    private static final String SEQUENCE_REDIS_KEY = "voum:realtime:sequence";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a real-time event message to the Redis Pub/Sub channel.
     * Generates a global sequence number using Redis increment to ensure ordering.
     */
    public void publish(String eventType, UUID requestId, List<UUID> driverIds, Object payload) {
        try {
            // Generate sequential sequence number using Redis increment
            Long sequence = redisTemplate.opsForValue().increment(SEQUENCE_REDIS_KEY);
            if (sequence == null) {
                sequence = 1L;
            }

            RealtimeEventMessage message = RealtimeEventMessage.builder()
                    .eventType(eventType)
                    .eventVersion(1)
                    .sequence(sequence)
                    .requestId(requestId)
                    .driverIds(driverIds)
                    .payload(payload)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisRealtimeConfig.REALTIME_CHANNEL, jsonMessage);
            
            log.debug("Published realtime event {} to Redis (sequence={})", eventType, sequence);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize realtime event payload for {}", eventType, e);
        }
    }
}
