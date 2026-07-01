package com.voum.modules.support.repository;

import com.voum.modules.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SupportTicket> findByAssignedAdminId(UUID adminId);

    long countByStatus(SupportTicket.TicketStatus status);

    long countByType(SupportTicket.TicketType type);

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (first_response_at - created_at))), 0.0) FROM support_tickets WHERE first_response_at IS NOT NULL", nativeQuery = true)
    Double getAverageFirstResponseTimeSeconds();

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))), 0.0) FROM support_tickets WHERE resolved_at IS NOT NULL", nativeQuery = true)
    Double getAverageResolutionTimeSeconds();
}
