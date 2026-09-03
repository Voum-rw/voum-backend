package com.voum.modules.subscription.repository;

import com.voum.modules.subscription.entity.MoMoTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MoMoTransactionRepository extends JpaRepository<MoMoTransaction, UUID> {

    Optional<MoMoTransaction> findByMomoTransactionId(String momoTransactionId);

    Optional<MoMoTransaction> findByExternalReference(String externalReference);

    @Query("SELECT t FROM MoMoTransaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:planId IS NULL OR t.plan.id = :planId) AND " +
           "(:query IS NULL OR LOWER(t.momoTransactionId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.externalReference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.motari.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.motari.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " t.phoneNumber LIKE CONCAT('%', :query, '%'))")
    Page<MoMoTransaction> searchTransactions(
            @Param("status") String status,
            @Param("planId") String planId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT SUM(t.amount) FROM MoMoTransaction t WHERE t.status = 'SUCCESSFUL' AND t.createdAt >= :since")
    Double sumSuccessfulAmountSince(@Param("since") Instant since);
}
