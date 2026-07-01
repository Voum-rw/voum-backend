package com.voum.modules.notification.repository;

import com.voum.modules.notification.entity.NotificationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.userId = :userId ORDER BY nl.createdAt DESC")
    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.status = 'PENDING' OR nl.status = 'FAILED' ORDER BY nl.createdAt ASC")
    List<NotificationLog> findPendingRetries();
}
