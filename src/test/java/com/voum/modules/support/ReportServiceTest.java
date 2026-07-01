package com.voum.modules.support;

import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import com.voum.modules.support.entity.UserReport;
import com.voum.modules.support.entity.UserReport.ReportSeverity;
import com.voum.modules.support.entity.UserReport.ReportStatus;
import com.voum.modules.support.repository.UserReportRepository;
import com.voum.modules.support.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReportService reportService;

    private final UUID reporterId = UUID.randomUUID();
    private final UUID reportedUserId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        reportService = new ReportService(
                userReportRepository,
                userRepository,
                eventPublisher
        );
    }

    @Test
    public void createReport_lowSeverity_shouldIncrementFlagByOne() {
        User reportedUser = User.builder()
                .id(reportedUserId)
                .flagCount(0)
                .isFlagged(false)
                .build();

        UserReport savedReport = UserReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .severity(ReportSeverity.LOW)
                .status(ReportStatus.PENDING)
                .build();

        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reportedUser));
        when(userReportRepository.save(any(UserReport.class))).thenReturn(savedReport);

        UserReport report = reportService.createReport(
                reporterId,
                reportedUserId,
                null,
                "Lateness",
                "Arrived 20 mins late",
                ReportSeverity.LOW
        );

        assertNotNull(report);
        assertEquals(1, reportedUser.getFlagCount());
        assertFalse(reportedUser.getIsFlagged());
        verify(userRepository).save(reportedUser);
        verify(eventPublisher).publishEvent(any(com.voum.modules.support.events.UserReportedEvent.class));
    }

    @Test
    public void createReport_criticalSeverity_shouldIncrementFlagByFiveAndMarkFlagged() {
        User reportedUser = User.builder()
                .id(reportedUserId)
                .flagCount(1)
                .isFlagged(false)
                .build();

        UserReport savedReport = UserReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .severity(ReportSeverity.CRITICAL)
                .status(ReportStatus.PENDING)
                .build();

        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reportedUser));
        when(userReportRepository.save(any(UserReport.class))).thenReturn(savedReport);

        UserReport report = reportService.createReport(
                reporterId,
                reportedUserId,
                null,
                "Safety risk",
                "Dangerous speeding and lane splitting",
                ReportSeverity.CRITICAL
        );

        assertNotNull(report);
        assertEquals(6, reportedUser.getFlagCount());
        assertTrue(reportedUser.getIsFlagged());
        verify(userRepository).save(reportedUser);
    }

    @Test
    public void resolveReport_shouldSetStatusToResolvedAndTrackOutcome() {
        UUID adminId = UUID.randomUUID();
        UserReport report = UserReport.builder()
                .id(reportId)
                .status(ReportStatus.PENDING)
                .build();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        reportService.resolveReport(reportId, adminId, "Warned driver about speeding.");

        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(adminId, report.getResolvedBy());
        assertEquals("Warned driver about speeding.", report.getResolutionSummary());
        assertNotNull(report.getResolvedAt());
        verify(userReportRepository).save(report);
    }
}
