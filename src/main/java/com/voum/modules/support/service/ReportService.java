package com.voum.modules.support.service;

import com.voum.common.ApiException;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import com.voum.modules.support.entity.UserReport;
import com.voum.modules.support.entity.UserReport.ReportSeverity;
import com.voum.modules.support.entity.UserReport.ReportStatus;
import com.voum.modules.support.repository.UserReportRepository;
import com.voum.modules.support.events.UserReportedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserReport createReport(UUID reporterId, UUID reportedUserId, UUID tripId, String reason, String description, ReportSeverity severity) {
        User reportedUser = userRepository.findById(reportedUserId)
                .orElseThrow(() -> new ApiException("Reported user not found.", HttpStatus.NOT_FOUND));

        UserReport report = UserReport.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .tripId(tripId)
                .reason(reason)
                .description(description)
                .severity(severity)
                .status(ReportStatus.PENDING)
                .build();

        report = userReportRepository.save(report);

        // Weighted Flag Arithmetic
        int increment = 1;
        switch (severity) {
            case LOW:
                increment = 1;
                break;
            case MEDIUM:
                increment = 2;
                break;
            case HIGH:
                increment = 3;
                break;
            case CRITICAL:
                increment = 5;
                break;
        }

        int newFlagCount = reportedUser.getFlagCount() + increment;
        reportedUser.setFlagCount(newFlagCount);

        log.info("Report severity={} added. User {} flag count incremented by +{}. Total flags: {}",
                severity, reportedUserId, increment, newFlagCount);

        // Fraud Threshold Logic
        if (newFlagCount >= 5) {
            reportedUser.setIsFlagged(true);
            log.warn("User {} flagged automatically due to flag count >= 5", reportedUserId);
        }

        if (newFlagCount >= 10) {
            log.warn("CRITICAL: User {} has reached {} flags. Pending suspension review override.",
                    reportedUserId, newFlagCount);
        } else if (newFlagCount >= 3) {
            log.info("User {} received a warning mark due to flag count >= 3", reportedUserId);
        }

        userRepository.save(reportedUser);

        eventPublisher.publishEvent(new UserReportedEvent(report));

        return report;
    }

    @Transactional
    public void resolveReport(UUID reportId, UUID adminId, String resolutionSummary) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException("Report not found.", HttpStatus.NOT_FOUND));

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedBy(adminId);
        report.setResolvedAt(Instant.now());
        report.setResolutionSummary(resolutionSummary);
        userReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<UserReport> getReportsAboutUser(UUID reportedUserId) {
        return userReportRepository.findByReportedUserIdOrderByCreatedAtDesc(reportedUserId);
    }

    @Transactional(readOnly = true)
    public List<UserReport> getMyReports(UUID reporterId) {
        return userReportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
    }

    @Transactional(readOnly = true)
    public List<UserReport> getAllReports() {
        return userReportRepository.findAll();
    }
}
