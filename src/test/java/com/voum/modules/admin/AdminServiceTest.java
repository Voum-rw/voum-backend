package com.voum.modules.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.common.ApiException;
import com.voum.modules.users.User;
import com.voum.modules.users.Role;
import com.voum.modules.users.UserRepository;
import com.voum.modules.users.PassengerRepository;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.repository.TripRepository;
import com.voum.modules.onboarding.VerificationRequest;
import com.voum.modules.onboarding.VerificationRequestRepository;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.marketplace.repository.RideOfferRepository;
import com.voum.modules.review.entity.TripReview;
import com.voum.modules.review.repository.TripReviewRepository;
import com.voum.modules.audit.entity.AuditLog;
import com.voum.modules.audit.repository.AuditLogRepository;
import com.voum.modules.audit.service.AuditLogService;
import com.voum.modules.admin.notes.entity.AdminNote;
import com.voum.modules.admin.notes.repository.AdminNoteRepository;
import com.voum.modules.admin.service.AdminService;
import com.voum.modules.admin.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private MotariRepository motariRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private RideOfferRepository rideOfferRepository;

    @Mock
    private TripReviewRepository tripReviewRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AdminNoteRepository adminNoteRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private com.voum.modules.support.repository.SupportTicketRepository supportTicketRepository;

    @Mock
    private com.voum.modules.support.repository.UserReportRepository userReportRepository;

    @Mock
    private com.voum.modules.support.repository.LostItemRepository lostItemRepository;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private AuditLogService auditLogService;
    private AdminService adminService;

    private final UUID adminId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        auditLogService = new AuditLogService(
                auditLogRepository,
                userRepository,
                new ObjectMapper()
        );

        adminService = new AdminService(
                userRepository,
                passengerRepository,
                motariRepository,
                tripRepository,
                verificationRequestRepository,
                userLocationRepository,
                rideRequestRepository,
                rideOfferRepository,
                tripReviewRepository,
                auditLogService,
                adminNoteRepository,
                redisTemplate,
                supportTicketRepository,
                userReportRepository,
                lostItemRepository
        );
    }

    // ── Suspension Tests ──────────────────────────────────────────────────────

    @Test
    public void suspendUser_shouldBlockUserAndCreateAuditLog() {
        User user = User.builder()
                .id(targetUserId)
                .isBlocked(false)
                .status("ACTIVE")
                .phone("+250780000000")
                .build();

        User adminUser = User.builder()
                .id(adminId)
                .phone("+250781111111")
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        adminService.suspendUser(targetUserId, "Fake verification document submitted", adminId);

        assertTrue(user.getIsBlocked());
        assertEquals("BLOCKED", user.getStatus());
        assertEquals("Fake verification document submitted", user.getSuspensionReason());
        assertEquals(adminId, user.getSuspendedBy());
        assertNotNull(user.getSuspendedAt());

        // Verify save user
        verify(userRepository).save(user);

        // Verify audit log creation
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertEquals(adminId, auditLog.getActorId());
        assertEquals("+250781111111", auditLog.getActorPhone());
        assertEquals("SUSPEND_USER", auditLog.getAction());
        assertEquals(targetUserId.toString(), auditLog.getTarget());
        assertEquals("USER", auditLog.getTargetType());
        assertTrue(auditLog.getMetadata().contains("Fake verification document"));
    }

    @Test
    public void suspendUser_alreadySuspended_shouldThrowException() {
        User user = User.builder()
                .id(targetUserId)
                .isBlocked(true)
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        ApiException exception = assertThrows(ApiException.class, () ->
                adminService.suspendUser(targetUserId, "Already suspended", adminId));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // ── Reactivation Tests ────────────────────────────────────────────────────

    @Test
    public void reactivateUser_shouldUnblockUserAndClearSuspensionDetails() {
        User user = User.builder()
                .id(targetUserId)
                .isBlocked(true)
                .status("BLOCKED")
                .suspensionReason("Stale request")
                .suspendedAt(Instant.now())
                .suspendedBy(adminId)
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        adminService.reactivateUser(targetUserId, adminId);

        assertFalse(user.getIsBlocked());
        assertEquals("ACTIVE", user.getStatus());
        assertNull(user.getSuspensionReason());
        assertNull(user.getSuspendedAt());
        assertNull(user.getSuspendedBy());

        verify(userRepository).save(user);
    }

    // ── Archiving (Soft Delete) Tests ─────────────────────────────────────────

    @Test
    public void archiveUser_shouldSetDeletedByAndCallDelete() {
        User user = User.builder()
                .id(targetUserId)
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        adminService.archiveUser(targetUserId, adminId);

        assertEquals(adminId, user.getDeletedBy());
        verify(userRepository).save(user);
        verify(userRepository).delete(user);
    }

    // ── Admin Notes Tests ─────────────────────────────────────────────────────

    @Test
    public void addAdminNote_shouldSaveNoteAndAuditAction() {
        when(userRepository.existsById(targetUserId)).thenReturn(true);

        adminService.addAdminNote(targetUserId, "Called user to verify moto plate mismatch", adminId);

        ArgumentCaptor<AdminNote> noteCaptor = ArgumentCaptor.forClass(AdminNote.class);
        verify(adminNoteRepository).save(noteCaptor.capture());

        AdminNote note = noteCaptor.getValue();
        assertEquals(targetUserId, note.getUserId());
        assertEquals("Called user to verify moto plate mismatch", note.getNote());
        assertEquals(adminId, note.getCreatedBy());

        // Verify audit log
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ── Conversion Funnel Metrics Tests ───────────────────────────────────────

    @Test
    public void getMarketplaceConversionFunnel_shouldCalculateRatiosCorrectly() {
        when(rideRequestRepository.count()).thenReturn(100L);
        when(rideRequestRepository.countRequestsWithOffers()).thenReturn(80L);
        when(rideRequestRepository.countRequestsAccepted()).thenReturn(60L);
        when(tripRepository.countByStartedAtIsNotNull()).thenReturn(50L);
        when(tripRepository.countByStatus("COMPLETED")).thenReturn(40L);

        FunnelMetricsResponse response = adminService.getMarketplaceConversionFunnel();

        assertEquals(100L, response.getRequestsCreated());
        assertEquals(80L, response.getRequestsWithOffers());
        assertEquals(60L, response.getRequestsAccepted());
        assertEquals(50L, response.getTripsStarted());
        assertEquals(40L, response.getTripsCompleted());

        assertEquals(80.0, response.getRequestToOfferRate());
        assertEquals(75.0, response.getOfferToAcceptRate());
        assertEquals(83.33, response.getAcceptToStartRate(), 0.01);
        assertEquals(80.0, response.getStartToCompleteRate());
        assertEquals(40.0, response.getOverallFunnelConversionRate());
    }

    // ── System Health Tests ───────────────────────────────────────────────────

    @Test
    public void getSystemHealth_allSystemsUp_shouldReturnUp() {
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);

        SystemHealthResponse health = adminService.getSystemHealth();

        assertEquals("UP", health.getDatabase());
        assertEquals("UP", health.getRedis());
        assertEquals("UP", health.getWebsocket());
        assertEquals("UP", health.getNotifications());
        assertEquals("UP", health.getStorage());
    }
}
