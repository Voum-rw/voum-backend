package com.voum.modules.location.repository;

import com.voum.modules.location.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {

    Optional<UserLocation> findByUserId(UUID userId);

    @Query("SELECT u FROM UserLocation u WHERE u.availabilityStatus = 'ONLINE' " +
           "AND u.latitude BETWEEN :minLat AND :maxLat " +
           "AND u.longitude BETWEEN :minLng AND :maxLng " +
           "AND u.lastSeenAt >= :cutoffTime")
    List<UserLocation> findDriversInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("cutoffTime") Instant cutoffTime
    );

    @Modifying
    @Query("UPDATE UserLocation u SET u.availabilityStatus = 'OFFLINE' " +
           "WHERE u.availabilityStatus IN ('ONLINE', 'BUSY') AND u.lastSeenAt < :cutoff")
    int markStaleUsersOffline(@Param("cutoff") Instant cutoff);

    long countByAvailabilityStatus(String status);
    long countByAvailabilityStatusIn(java.util.List<String> statuses);
}

