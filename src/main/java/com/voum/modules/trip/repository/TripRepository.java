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
}
