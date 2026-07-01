package com.voum.modules.tracking.repository;

import com.voum.modules.tracking.entity.TripTrackingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripTrackingPointRepository extends JpaRepository<TripTrackingPoint, UUID> {

    @Query("SELECT t FROM TripTrackingPoint t WHERE t.tripId = :tripId ORDER BY t.sequenceNumber DESC")
    List<TripTrackingPoint> findTop100ByTripIdOrderBySequenceNumberDesc(@Param("tripId") UUID tripId);

    @Modifying
    @Query("DELETE FROM TripTrackingPoint t WHERE t.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
