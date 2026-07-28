package com.voum.modules.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationSessionRepository extends JpaRepository<VerificationSession, UUID> {
    Optional<VerificationSession> findTopByMotariIdOrderByStartedAtDesc(UUID motariId);
    List<VerificationSession> findByMotariId(UUID motariId);
    List<VerificationSession> findByStatus(String status);
}
