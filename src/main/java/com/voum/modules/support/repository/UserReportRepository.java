package com.voum.modules.support.repository;

import com.voum.modules.support.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, UUID> {
    List<UserReport> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);
    List<UserReport> findByReportedUserIdOrderByCreatedAtDesc(UUID reportedUserId);
    long countByStatus(UserReport.ReportStatus status);
}
