package com.voum.modules.location.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationCacheService {

    private static final Logger log = LoggerFactory.getLogger(LocationCacheService.class);
    private static final String ONLINE_MOTARIS_KEY = "voum:location:online_motaris";
    private static final String LOOKUP_CACHE_PREFIX = "voum:location:lookup:";

    private final StringRedisTemplate redisTemplate;

    public void cacheOnlineMotari(UUID userId) {
        try {
            redisTemplate.opsForSet().add(ONLINE_MOTARIS_KEY, userId.toString());
            log.debug("Cached online motari ID: {}", userId);
        } catch (Exception e) {
            log.warn("Redis write failed for online motari cache. Falling back to DB. Error: {}", e.getMessage());
        }
    }

    public void evictOnlineMotari(UUID userId) {
        try {
            redisTemplate.opsForSet().remove(ONLINE_MOTARIS_KEY, userId.toString());
            log.debug("Evicted online motari ID: {}", userId);
        } catch (Exception e) {
            log.warn("Redis delete failed for online motari cache. Falling back to DB. Error: {}", e.getMessage());
        }
    }

    public Set<UUID> getOnlineMotariIds() {
        try {
            Set<String> members = redisTemplate.opsForSet().members(ONLINE_MOTARIS_KEY);
            if (members != null) {
                return members.stream().map(UUID::fromString).collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("Redis read failed for online motaris list. Falling back to DB. Error: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    public void cacheNearbyQuery(String queryKey, String responseJson, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    LOOKUP_CACHE_PREFIX + queryKey,
                    responseJson,
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (Exception e) {
            log.warn("Redis write failed for nearby query cache. Error: {}", e.getMessage());
        }
    }

    public String getCachedNearbyQuery(String queryKey) {
        try {
            return redisTemplate.opsForValue().get(LOOKUP_CACHE_PREFIX + queryKey);
        } catch (Exception e) {
            log.warn("Redis read failed for cached nearby query. Error: {}", e.getMessage());
        }
        return null;
    }
}
