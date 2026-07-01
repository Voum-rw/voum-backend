package com.voum.modules.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    long countByRole(Role role);
    long countByRoleAndCreatedAtAfter(Role role, java.time.Instant time);
    long countByRoleAndIsVerifiedTrue(Role role);
    long countByCreatedAtAfter(java.time.Instant time);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:phone IS NULL OR u.phone LIKE %:phone%)")
    org.springframework.data.domain.Page<User> findAllFiltered(
            @org.springframework.data.repository.query.Param("role") Role role,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("phone") String phone,
            org.springframework.data.domain.Pageable pageable);
}

