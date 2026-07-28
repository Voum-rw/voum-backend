package com.voum.modules.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MotariVerificationProfileRepository extends JpaRepository<MotariVerificationProfile, UUID> {
    Optional<MotariVerificationProfile> findByPlateNumber(String plateNumber);
}
