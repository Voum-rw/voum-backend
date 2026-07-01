package com.voum.modules.admin.service;

import com.voum.common.ApiException;
import com.voum.modules.users.User;
import com.voum.modules.users.Role;
import com.voum.modules.users.Passenger;
import com.voum.modules.users.Motari;
import com.voum.modules.users.UserRepository;
import com.voum.modules.users.PassengerRepository;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.repository.TripRepository;
import com.voum.modules.onboarding.VerificationRequest;
import com.voum.modules.onboarding.VerificationRequestRepository;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.repository.RideRequestRepository;
import com.voum.modules.marketplace.repository.RideOfferRepository;
import com.voum.modules.review.entity.TripReview;
import com.voum.modules.review.repository.TripReviewRepository;
import com.voum.modules.audit.entity.AuditLog;
import com.voum.modules.audit.service.AuditLogService;
import com.voum.modules.admin.notes.entity.AdminNote;
import com.voum.modules.admin.notes.repository.AdminNoteRepository;
import com.voum.modules.admin.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MotariRepository motariRepository;
    private final TripRepository tripRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final UserLocationRepository userLocationRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RideOfferRepository rideOfferRepository;
    private final TripReviewRepository tripReviewRepository;
    private final AuditLogService auditLogService;
    private final AdminNoteRepository adminNoteRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long passengers = userRepository.countByRole(Role.PASSENGER);
        long motaris = userRepository.countByRole(Role.MOTARI);
        long verified = userRepository.countByRoleAndIsVerifiedTrue(Role.MOTARI);
        long pendingVerifications = verificationRequestRepository.countByStatus("PENDING");

        long completedTrips = tripRepository.countByStatus("COMPLETED");
        long cancelledTrips = tripRepository.countByStatus("CANCELLED");
        long totalTrips = tripRepository.count();
        long activeTrips = totalTrips - (completedTrips + cancelledTrips);

        long openRequests = rideRequestRepository.countActiveOpenRequests(Instant.now());
        long onlineMotaris = userLocationRepository.countByAvailabilityStatus("ONLINE");

        // Driver Verification KPIs
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long approvedToday = verificationRequestRepository.countByStatusAndUpdatedAtAfter("APPROVED", startOfToday);
        long rejectedToday = verificationRequestRepository.countByStatusAndUpdatedAtAfter("REJECTED", startOfToday);
        long totalProcessedToday = approvedToday + rejectedToday;

        double approvalRate = totalProcessedToday > 0 ? ((double) approvedToday / totalProcessedToday) * 100.0 : 100.0;
        double averageVerificationTime = verificationRequestRepository.getAverageVerificationTimeSeconds();

        return AdminDashboardResponse.builder()
                .totalPassengers(passengers)
                .totalMotaris(motaris)
                .verifiedMotaris(verified)
                .pendingVerifications(pendingVerifications)
                .activeTrips(activeTrips)
                .completedTrips(completedTrips)
                .cancelledTrips(cancelledTrips)
                .openRideRequests(openRequests)
                .activeOnlineMotaris(onlineMotaris)
                .approvedTodayCount(approvedToday)
                .rejectedTodayCount(rejectedToday)
                .approvalRate(approvalRate)
                .averageVerificationTimeSeconds(averageVerificationTime)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(Role role, String status, String phone, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAllFiltered(role, status, phone, pageable);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailsResponse getUserDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        AdminUserDetailsResponse.AdminUserDetailsResponseBuilder builder = AdminUserDetailsResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .rating(user.getRating())
                .completedTrips(user.getCompletedTrips())
                .isOnline(user.getIsOnline())
                .isVerified(user.getIsVerified())
                .isBlocked(user.getIsBlocked())
                .subscriptionPlan(user.getSubscriptionPlan())
                .suspensionReason(user.getSuspensionReason())
                .suspendedAt(user.getSuspendedAt())
                .suspendedBy(user.getSuspendedBy())
                .deletedBy(user.getDeletedBy())
                .flagCount(user.getFlagCount())
                .isFlagged(user.getIsFlagged())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (user.getRole() == Role.PASSENGER) {
            passengerRepository.findById(userId).ifPresent(p -> {
                builder.passengerFirstName(p.getFirstName())
                       .passengerLastName(p.getLastName())
                       .passengerProfileImage(p.getProfileImage());
            });
        } else if (user.getRole() == Role.MOTARI) {
            motariRepository.findById(userId).ifPresent(m -> {
                builder.motariFirstName(m.getFirstName())
                       .motariLastName(m.getLastName())
                       .motariNationalId(m.getNationalId())
                       .motariMotoPlateNumber(m.getMotoPlateNumber())
                       .motariProfileImage(m.getProfileImage())
                       .motariVerificationStatus(m.getVerificationStatus())
                       .motariAverageRating(m.getAverageRating())
                       .motariTotalReviews(m.getTotalReviews())
                       .motariCompletionRate(m.getCompletionRate())
                       .motariAcceptanceRate(m.getAcceptanceRate())
                       .motariCancellationRate(m.getCancellationRate())
                       .motariTrustScore(m.getTrustScore());
            });
        }

        return builder.build();
    }

    @Transactional
    public void suspendUser(UUID userId, String reason, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (user.getIsBlocked()) {
            throw new ApiException("User is already suspended.", HttpStatus.BAD_REQUEST);
        }

        user.setStatus("BLOCKED");
        user.setIsBlocked(true);
        user.setSuspensionReason(reason);
        user.setSuspendedAt(Instant.now());
        user.setSuspendedBy(adminId);
        userRepository.save(user);

        // Audit Logging with metadata
        auditLogService.logAction(adminId, "SUSPEND_USER", userId.toString(), "USER", Map.of(
                "reason", reason,
                "previousStatus", "ACTIVE"
        ));
    }

    @Transactional
    public void reactivateUser(UUID userId, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (!user.getIsBlocked()) {
            throw new ApiException("User is not suspended.", HttpStatus.BAD_REQUEST);
        }

        user.setStatus("ACTIVE");
        user.setIsBlocked(false);
        user.setSuspensionReason(null);
        user.setSuspendedAt(null);
        user.setSuspendedBy(null);
        userRepository.save(user);

        // Audit Logging
        auditLogService.logAction(adminId, "REACTIVATE_USER", userId.toString(), "USER", Map.of(
                "previousStatus", "BLOCKED"
        ));
    }

    @Transactional
    public void archiveUser(UUID userId, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        user.setDeletedBy(adminId);
        userRepository.save(user);
        userRepository.delete(user); // Triggers soft delete update clause

        // Audit Logging
        auditLogService.logAction(adminId, "ARCHIVE_USER", userId.toString(), "USER", null);
    }

    @Transactional(readOnly = true)
    public List<VerificationRequest> getPendingVerifications(String phone) {
        if (phone != null && !phone.isBlank()) {
            return verificationRequestRepository.findPendingByMotariPhone(phone);
        }
        return verificationRequestRepository.findByStatus("PENDING");
    }

    @Transactional(readOnly = true)
    public Page<Trip> getTrips(String status, String phone, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tripRepository.findAllFiltered(status, phone, pageable);
    }

    @Transactional(readOnly = true)
    public TripAdminResponse getTripDetails(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Trip not found.", HttpStatus.NOT_FOUND));

        User passengerUser = userRepository.findById(trip.getPassengerId()).orElse(null);
        User motariUser = userRepository.findById(trip.getMotariId()).orElse(null);
        Motari motariProfile = motariRepository.findById(trip.getMotariId()).orElse(null);

        TripAdminResponse.ParticipantDetails passengerDetails = passengerUser != null ?
                TripAdminResponse.ParticipantDetails.builder()
                        .id(passengerUser.getId())
                        .name(passengerUser.getName())
                        .phone(passengerUser.getPhone())
                        .rating(passengerUser.getRating())
                        .build() : null;

        TripAdminResponse.ParticipantDetails motariDetails = motariUser != null ?
                TripAdminResponse.ParticipantDetails.builder()
                        .id(motariUser.getId())
                        .name(motariUser.getName())
                        .phone(motariUser.getPhone())
                        .rating(motariUser.getRating())
                        .motoPlateNumber(motariProfile != null ? motariProfile.getMotoPlateNumber() : null)
                        .build() : null;

        // Build Chronological Timeline Milestones Map
        Map<String, Instant> timeline = new LinkedHashMap<>();
        timeline.put("CREATED", trip.getCreatedAt());
        if (trip.getStartedAt() != null) timeline.put("STARTED", trip.getStartedAt());
        if (trip.getCompletedAt() != null) timeline.put("COMPLETED", trip.getCompletedAt());
        if (trip.getCancelledAt() != null) timeline.put("CANCELLED", trip.getCancelledAt());

        return TripAdminResponse.builder()
                .id(trip.getId())
                .tripNumber(trip.getTripNumber())
                .rideRequestId(trip.getRideRequestId())
                .rideOfferId(trip.getRideOfferId())
                .status(trip.getStatus())
                .agreedPrice(trip.getAgreedPrice())
                .estimatedArrivalMinutes(trip.getEstimatedArrivalMinutes())
                .estimatedDistanceKm(trip.getEstimatedDistanceKm())
                .pickupAddress(trip.getPickupAddress())
                .destinationAddress(trip.getDestinationAddress())
                .cancellationReason(trip.getCancellationReason())
                .cancelledBy(trip.getCancelledBy())
                .createdAt(trip.getCreatedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .timeline(timeline)
                .passenger(passengerDetails)
                .motari(motariDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TripReview> getReviews(String phone, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tripReviewRepository.findAllFiltered(phone, pageable);
    }

    @Transactional(readOnly = true)
    public MarketplaceMetricsResponse getMarketplaceMetrics() {
        long matched = rideRequestRepository.countByStatus("MATCHED");
        long expired = rideRequestRepository.countByStatus("EXPIRED");
        long cancelled = rideRequestRepository.countByStatus("CANCELLED");
        long totalTerminal = matched + expired + cancelled;

        double successRate = totalTerminal > 0 ? ((double) matched / totalTerminal) * 100.0 : 100.0;
        double responseTime = rideOfferRepository.getAverageResponseTimeSeconds();

        long open = rideRequestRepository.countActiveOpenRequests(Instant.now());
        long activeOffers = rideOfferRepository.countByStatus("PENDING");

        return MarketplaceMetricsResponse.builder()
                .matchingSuccessRate(successRate)
                .averageOfferResponseTimeSeconds(responseTime)
                .openRequestsCount(open)
                .activeOffersCount(activeOffers)
                .build();
    }

    @Transactional(readOnly = true)
    public FunnelMetricsResponse getMarketplaceConversionFunnel() {
        long requestsCreated = rideRequestRepository.count();
        long requestsWithOffers = rideRequestRepository.countRequestsWithOffers();
        long requestsAccepted = rideRequestRepository.countRequestsAccepted();
        long tripsStarted = tripRepository.countByStartedAtIsNotNull();
        long tripsCompleted = tripRepository.countByStatus("COMPLETED");

        double reqToOffer = requestsCreated > 0 ? ((double) requestsWithOffers / requestsCreated) * 100.0 : 0.0;
        double offerToAccept = requestsWithOffers > 0 ? ((double) requestsAccepted / requestsWithOffers) * 100.0 : 0.0;
        double acceptToStart = requestsAccepted > 0 ? ((double) tripsStarted / requestsAccepted) * 100.0 : 0.0;
        double startToComplete = tripsStarted > 0 ? ((double) tripsCompleted / tripsStarted) * 100.0 : 0.0;
        double overallFunnel = requestsCreated > 0 ? ((double) tripsCompleted / requestsCreated) * 100.0 : 0.0;

        return FunnelMetricsResponse.builder()
                .requestsCreated(requestsCreated)
                .requestsWithOffers(requestsWithOffers)
                .requestsAccepted(requestsAccepted)
                .tripsStarted(tripsStarted)
                .tripsCompleted(tripsCompleted)
                .requestToOfferRate(reqToOffer)
                .offerToAcceptRate(offerToAccept)
                .acceptToStartRate(acceptToStart)
                .startToCompleteRate(startToComplete)
                .overallFunnelConversionRate(overallFunnel)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsMetricsResponse getAnalytics(String periodStr) {
        Instant startDate;
        String period = periodStr.toUpperCase();
        switch (period) {
            case "DAILY":
                startDate = Instant.now().minus(24, ChronoUnit.HOURS);
                break;
            case "WEEKLY":
                startDate = Instant.now().minus(7, ChronoUnit.DAYS);
                break;
            case "MONTHLY":
            default:
                period = "MONTHLY";
                startDate = Instant.now().minus(30, ChronoUnit.DAYS);
                break;
        }

        long newUsers = userRepository.countByCreatedAtAfter(startDate);
        long newMotaris = userRepository.countByRoleAndCreatedAtAfter(Role.MOTARI, startDate);
        long totalTrips = tripRepository.countByCreatedAtAfter(startDate);
        long completedTrips = tripRepository.countByStatusAndCreatedAtAfter("COMPLETED", startDate);
        long cancelledTrips = tripRepository.countByStatusAndCreatedAtAfter("CANCELLED", startDate);
        long offersSubmitted = rideOfferRepository.countByCreatedAtAfter(startDate);

        double completionRate = totalTrips > 0 ? ((double) completedTrips / totalTrips) * 100.0 : 0.0;
        double cancellationRate = totalTrips > 0 ? ((double) cancelledTrips / totalTrips) * 100.0 : 0.0;

        return AnalyticsMetricsResponse.builder()
                .period(period)
                .newUsers(newUsers)
                .newMotaris(newMotaris)
                .totalTrips(totalTrips)
                .completedTrips(completedTrips)
                .cancelledTrips(cancelledTrips)
                .offersSubmitted(offersSubmitted)
                .completionRate(completionRate)
                .cancellationRate(cancellationRate)
                .build();
    }

    @Transactional(readOnly = true)
    public SystemHealthResponse getSystemHealth() {
        String dbStatus = "UP";
        try {
            userRepository.count();
        } catch (Exception e) {
            dbStatus = "DOWN: " + e.getMessage();
        }

        String redisStatus = "UP";
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
        } catch (Exception e) {
            redisStatus = "DOWN: " + e.getMessage();
        }

        return SystemHealthResponse.builder()
                .database(dbStatus)
                .redis(redisStatus)
                .websocket("UP")
                .notifications("UP")
                .storage("UP")
                .build();
    }

    // ── Admin Notes Endpoints ────────────────────────────────────────────────

    @Transactional
    public void addAdminNote(UUID userId, String noteText, UUID adminId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException("User not found.", HttpStatus.NOT_FOUND);
        }

        AdminNote note = AdminNote.builder()
                .userId(userId)
                .note(noteText)
                .createdBy(adminId)
                .build();

        adminNoteRepository.save(note);

        // Audit log action
        auditLogService.logAction(adminId, "ADD_ADMIN_NOTE", userId.toString(), "USER_NOTE", Map.of(
                "noteLength", noteText.length()
        ));
    }

    @Transactional(readOnly = true)
    public List<AdminNote> getAdminNotes(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException("User not found.", HttpStatus.NOT_FOUND);
        }
        return adminNoteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
