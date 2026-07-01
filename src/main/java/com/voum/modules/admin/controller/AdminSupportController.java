package com.voum.modules.admin.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.support.entity.SupportTicket;
import com.voum.modules.support.entity.UserReport;
import com.voum.modules.support.entity.LostItem;
import com.voum.modules.support.service.SupportService;
import com.voum.modules.support.service.ReportService;
import com.voum.modules.support.service.LostItemService;
import com.voum.modules.support.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportService supportService;
    private final ReportService reportService;
    private final LostItemService lostItemService;

    // ── Support Tickets Endpoints ─────────────────────────────────────────────

    @GetMapping("/support/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getAllTickets() {
        List<SupportTicket> tickets = supportService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets, "All support tickets retrieved."));
    }

    @GetMapping("/support/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicket>> getTicketDetails(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID adminId
    ) {
        SupportTicket ticket = supportService.getTicketDetails(ticketId, adminId);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Support ticket details retrieved."));
    }

    @PostMapping("/support/tickets/{id}/assign")
    public ResponseEntity<ApiResponse<Void>> assignTicket(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID actorAdminId,
            @Valid @RequestBody AssignTicketRequest req
    ) {
        supportService.assignTicket(ticketId, req.getAssignedAdminId(), actorAdminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Ticket assigned successfully."));
    }

    @PostMapping("/support/tickets/{id}/close")
    public ResponseEntity<ApiResponse<Void>> closeTicket(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody CloseTicketRequest req
    ) {
        supportService.closeTicket(ticketId, adminId, req.getResolutionSummary());
        return ResponseEntity.ok(ApiResponse.success(null, "Ticket closed successfully."));
    }

    @PostMapping("/support/tickets/{id}/notes")
    public ResponseEntity<ApiResponse<Void>> updateInternalNotes(
            @PathVariable("id") UUID ticketId,
            @RequestBody Map<String, String> body
    ) {
        String notes = body.get("notes");
        supportService.updateInternalNotes(ticketId, notes);
        return ResponseEntity.ok(ApiResponse.success(null, "Internal admin notes updated."));
    }

    // ── Reports Endpoints ─────────────────────────────────────────────────────

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<UserReport>>> getAllReports() {
        List<UserReport> reports = reportService.getAllReports();
        return ResponseEntity.ok(ApiResponse.success(reports, "All user reports retrieved."));
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable("id") UUID reportId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody ResolveReportRequest req
    ) {
        reportService.resolveReport(reportId, adminId, req.getResolutionSummary());
        return ResponseEntity.ok(ApiResponse.success(null, "User report resolved successfully."));
    }

    // ── Lost Items Endpoints ──────────────────────────────────────────────────

    @GetMapping("/lost-items")
    public ResponseEntity<ApiResponse<List<LostItem>>> getAllLostItems() {
        List<LostItem> items = lostItemService.getAllLostItems();
        return ResponseEntity.ok(ApiResponse.success(items, "All lost items retrieved."));
    }

    @PostMapping("/lost-items/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateLostItemStatus(
            @PathVariable("id") UUID itemId,
            @Valid @RequestBody UpdateLostItemStatusRequest req
    ) {
        lostItemService.updateStatus(itemId, req.getStatus());
        return ResponseEntity.ok(ApiResponse.success(null, "Lost item status updated successfully."));
    }
}
