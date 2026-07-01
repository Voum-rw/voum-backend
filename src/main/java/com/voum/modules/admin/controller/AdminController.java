package com.voum.modules.admin.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.users.User;
import com.voum.modules.users.Role;
import com.voum.modules.onboarding.VerificationRequest;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.review.entity.TripReview;
import com.voum.modules.audit.entity.AuditLog;
import com.voum.modules.audit.service.AuditLogService;
import com.voum.modules.admin.notes.entity.AdminNote;
import com.voum.modules.admin.service.AdminService;
import com.voum.modules.admin.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard metrics retrieved successfully."));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> listUsers(
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<User> response = adminService.listUsers(role, status, phone, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Users list retrieved successfully."));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailsResponse>> getUserDetails(
            @PathVariable("id") UUID userId
    ) {
        AdminUserDetailsResponse response = adminService.getUserDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User details retrieved successfully."));
    }

    @PostMapping("/users/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody SuspendRequest req
    ) {
        adminService.suspendUser(userId, req.getReason(), adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "User suspended successfully."));
    }

    @PostMapping("/users/{id}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal UUID adminId
    ) {
        adminService.reactivateUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "User account reactivated successfully."));
    }

    @PostMapping("/users/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archiveUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal UUID adminId
    ) {
        adminService.archiveUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "User account archived (soft-deleted) successfully."));
    }

    @PostMapping("/users/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addAdminNote(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal UUID adminId,
            @RequestBody Map<String, String> body
    ) {
        String note = body.get("note");
        if (note == null || note.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Note text is required."));
        }
        adminService.addAdminNote(userId, note, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Admin note recorded successfully."));
    }

    @GetMapping("/users/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminNote>>> getAdminNotes(
            @PathVariable("id") UUID userId
    ) {
        List<AdminNote> notes = adminService.getAdminNotes(userId);
        return ResponseEntity.ok(ApiResponse.success(notes, "Admin notes retrieved successfully."));
    }

    @GetMapping("/verifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<VerificationRequest>>> getPendingVerifications(
            @RequestParam(value = "phone", required = false) String phone
    ) {
        List<VerificationRequest> requests = adminService.getPendingVerifications(phone);
        return ResponseEntity.ok(ApiResponse.success(requests, "Pending verifications retrieved successfully."));
    }

    @GetMapping("/trips")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<Trip>>> getTrips(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<Trip> trips = adminService.getTrips(status, phone, page, size);
        return ResponseEntity.ok(ApiResponse.success(trips, "Trips list retrieved successfully."));
    }

    @GetMapping("/trips/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TripAdminResponse>> getTripDetails(
            @PathVariable("id") UUID tripId
    ) {
        TripAdminResponse response = adminService.getTripDetails(tripId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip details retrieved successfully."));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<TripReview>>> getReviews(
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<TripReview> reviews = adminService.getReviews(phone, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews, "Reviews list retrieved successfully."));
    }

    @GetMapping("/marketplace/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MarketplaceMetricsResponse>> getMarketplaceMetrics() {
        MarketplaceMetricsResponse response = adminService.getMarketplaceMetrics();
        return ResponseEntity.ok(ApiResponse.success(response, "Marketplace metrics retrieved successfully."));
    }

    @GetMapping("/marketplace/funnel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FunnelMetricsResponse>> getMarketplaceFunnel() {
        FunnelMetricsResponse response = adminService.getMarketplaceConversionFunnel();
        return ResponseEntity.ok(ApiResponse.success(response, "Marketplace conversion funnel retrieved successfully."));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsMetricsResponse>> getAnalytics(
            @RequestParam(value = "period", defaultValue = "MONTHLY") String period
    ) {
        AnalyticsMetricsResponse response = adminService.getAnalytics(period);
        return ResponseEntity.ok(ApiResponse.success(response, "Period analytics retrieved successfully."));
    }

    @GetMapping("/system-health")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getSystemHealth() {
        SystemHealthResponse response = adminService.getSystemHealth();
        return ResponseEntity.ok(ApiResponse.success(response, "System health checklist retrieved successfully."));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<AuditLog> response = auditLogService.getAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Audit logs page retrieved successfully."));
    }
}
