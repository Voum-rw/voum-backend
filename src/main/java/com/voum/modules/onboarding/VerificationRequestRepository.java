package com.voum.modules.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {
    List<VerificationRequest> findByStatus(String status);
    Optional<VerificationRequest> findByMotariIdAndStatus(UUID motariId, String status);

    long countByStatus(String status);
    long countByStatusAndUpdatedAtAfter(String status, java.time.Instant time);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (vr.updatedAt - vr.createdAt))), 0.0) FROM VerificationRequest vr WHERE vr.status IN ('APPROVED', 'REJECTED')")
    Double getAverageVerificationTimeSeconds();

    @org.springframework.data.jpa.repository.Query("SELECT vr FROM VerificationRequest vr WHERE vr.status = 'PENDING' AND vr.motariId IN (SELECT m.id FROM Motari m WHERE m.phoneNumber LIKE %:phone%)")
    List<VerificationRequest> findPendingByMotariPhone(@org.springframework.data.repository.query.Param("phone") String phone);
}

