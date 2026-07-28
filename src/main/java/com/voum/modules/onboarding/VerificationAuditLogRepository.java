package com.voum.modules.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationAuditLogRepository extends JpaRepository<VerificationAuditLog, UUID> {
    List<VerificationAuditLog> findBySessionId(UUID sessionId);
    List<VerificationAuditLog> findByDocumentId(UUID documentId);
}
