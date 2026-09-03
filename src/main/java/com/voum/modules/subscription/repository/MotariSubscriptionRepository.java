package com.voum.modules.subscription.repository;

import com.voum.modules.subscription.entity.MotariSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MotariSubscriptionRepository extends JpaRepository<MotariSubscription, UUID> {

    Optional<MotariSubscription> findTopByMotariIdOrderByExpiryDateDesc(UUID motariId);

    List<MotariSubscription> findByStatus(String status);

    @Query("SELECT s FROM MotariSubscription s WHERE s.status = 'ACTIVE' AND s.expiryDate <= :threshold")
    List<MotariSubscription> findExpiringSoon(@Param("threshold") Instant threshold);

    @Query("SELECT COUNT(s) FROM MotariSubscription s WHERE s.status = 'ACTIVE'")
    long countActiveSubscriptions();

    @Query("SELECT SUM(s.amountPaid) FROM MotariSubscription s WHERE s.status = 'ACTIVE'")
    Double calculateTotalActiveMRR();
}
