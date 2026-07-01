package com.voum.modules.trip.repository;

import com.voum.modules.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    @Query("SELECT t FROM Trip t WHERE t.passengerId = :userId OR t.motariId = :userId ORDER BY t.createdAt DESC")
    List<Trip> findAllByPassengerIdOrMotariId(@Param("userId") UUID userId);

    List<Trip> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);

    List<Trip> findByMotariIdOrderByCreatedAtDesc(UUID motariId);

    long countByMotariIdAndStatus(UUID motariId, String status);
    long countByMotariIdAndStatusAndCancelledBy(UUID motariId, String status, UUID cancelledBy);
    long countByPassengerIdAndStatus(UUID passengerId, String status);
    long countByPassengerIdAndStatusAndCancelledBy(UUID passengerId, String status, UUID cancelledBy);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.motariId = :motariId AND (t.status = 'COMPLETED' OR t.status = 'CANCELLED')")
    long countTerminalTripsForMotari(@Param("motariId") UUID motariId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.passengerId = :passengerId AND (t.status = 'COMPLETED' OR t.status = 'CANCELLED')")
    long countTerminalTripsForPassenger(@Param("passengerId") UUID passengerId);

    long countByCreatedAtAfter(java.time.Instant time);
    long countByStatus(String status);
    long countByStatusAndCreatedAtAfter(String status, java.time.Instant time);

    @Query("SELECT t FROM Trip t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:phone IS NULL OR " +
           " t.passengerId IN (SELECT u.id FROM User u WHERE u.phone LIKE CONCAT('%', :phone, '%')) OR " +
           " t.motariId IN (SELECT u.id FROM User u WHERE u.phone LIKE CONCAT('%', :phone, '%')))")
    org.springframework.data.domain.Page<Trip> findAllFiltered(
            @Param("status") String status,
            @Param("phone") String phone,
            org.springframework.data.domain.Pageable pageable);

    long countByStartedAtIsNotNull();
}




