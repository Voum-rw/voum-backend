package com.voum.modules.marketplace.repository;

import com.voum.modules.marketplace.entity.RideRequest;
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
public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {

    List<RideRequest> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);

    boolean existsByPassengerIdAndStatus(UUID passengerId, String status);

    List<RideRequest> findByStatusAndExpiresAtBefore(String status, Instant now);

    @Query("SELECT r FROM RideRequest r WHERE r.status = 'OPEN' " +
           "AND r.pickupLatitude BETWEEN :minLat AND :maxLat " +
           "AND r.pickupLongitude BETWEEN :minLng AND :maxLng " +
           "AND r.expiresAt > :now")
    List<RideRequest> findOpenRequestsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("now") Instant now
    );
}
