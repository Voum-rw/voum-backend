package com.voum.modules.marketplace.repository;

import com.voum.modules.marketplace.entity.RideOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideOfferRepository extends JpaRepository<RideOffer, UUID> {

    List<RideOffer> findByRideRequestId(UUID rideRequestId);

    List<RideOffer> findByRideRequestIdAndStatus(UUID rideRequestId, String status);

    Optional<RideOffer> findByRideRequestIdAndMotariId(UUID rideRequestId, UUID motariId);

    Optional<RideOffer> findByRideRequestIdAndMotariIdAndStatus(UUID rideRequestId, UUID motariId, String status);

    boolean existsByRideRequestIdAndMotariIdAndStatus(UUID rideRequestId, UUID motariId, String status);

    long countByMotariId(UUID motariId);
    long countByMotariIdAndStatus(UUID motariId, String status);

    long countByCreatedAtAfter(java.time.Instant time);
    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (o.created_at - r.created_at))), 0.0) FROM ride_offers o JOIN ride_requests r ON o.ride_request_id = r.id", nativeQuery = true)
    Double getAverageResponseTimeSeconds();
}


