package com.voum.modules.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MotariRepository extends JpaRepository<Motari, UUID> {
    Optional<Motari> findByMotoPlateNumber(String motoPlateNumber);
    Optional<Motari> findByNationalId(String nationalId);
    boolean existsByMotoPlateNumber(String motoPlateNumber);
    boolean existsByNationalId(String nationalId);
}
