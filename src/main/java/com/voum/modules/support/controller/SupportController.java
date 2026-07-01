package com.voum.modules.support.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.support.entity.SupportTicket;
import com.voum.modules.support.entity.TicketMessage;
import com.voum.modules.support.entity.UserReport;
import com.voum.modules.support.entity.LostItem;
import com.voum.modules.support.service.SupportService;
import com.voum.modules.support.service.ReportService;
import com.voum.modules.support.service.LostItemService;
import com.voum.modules.support.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;
    private final ReportService reportService;
    private final LostItemService lostItemService;

    // ── Support Tickets Endpoints ─────────────────────────────────────────────

    @PostMapping("/support/tickets")
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateTicketRequest req
    ) {
        SupportTicket ticket = supportService.createTicket(
                userId,
                req.getType(),
                req.getTripId(),
                req.getSubject(),
                req.getDescription(),
                req.getFileUrls(),
                req.getFileTypes()
        );
        return ResponseEntity.ok(ApiResponse.success(ticket, "Support ticket created successfully."));
    }

    @GetMapping("/support/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getMyTickets(
            @AuthenticationPrincipal UUID userId
    ) {
        List<SupportTicket> tickets = supportService.getUserTickets(userId);
        return ResponseEntity.ok(ApiResponse.success(tickets, "Support tickets retrieved successfully."));
    }

    @GetMapping("/support/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicket>> getTicketDetails(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID userId
    ) {
        SupportTicket ticket = supportService.getTicketDetails(ticketId, userId);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Support ticket details retrieved."));
    }

    @GetMapping("/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<List<TicketMessage>>> getTicketMessages(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID userId
    ) {
        List<TicketMessage> messages = supportService.getTicketMessages(ticketId, userId);
        return ResponseEntity.ok(ApiResponse.success(messages, "Ticket messages retrieved successfully."));
    }

    @PostMapping("/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessage>> replyToTicket(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ReplyMessageRequest req
    ) {
        TicketMessage message = supportService.replyToTicket(ticketId, userId, req.getMessage());
        return ResponseEntity.ok(ApiResponse.success(message, "Reply sent successfully."));
    }

    @PostMapping("/support/tickets/{id}/reopen")
    public ResponseEntity<ApiResponse<Void>> reopenTicket(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UUID userId
    ) {
        supportService.reopenTicket(ticketId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Ticket reopened successfully."));
    }

    // ── Reports Endpoints ─────────────────────────────────────────────────────

    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<UserReport>> createReport(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateReportRequest req
    ) {
        UserReport report = reportService.createReport(
                userId,
                req.getReportedUserId(),
                req.getTripId(),
                req.getReason(),
                req.getDescription(),
                req.getSeverity()
        );
        return ResponseEntity.ok(ApiResponse.success(report, "User report filed successfully."));
    }

    @GetMapping("/reports/my")
    public ResponseEntity<ApiResponse<List<UserReport>>> getMyReports(
            @AuthenticationPrincipal UUID userId
    ) {
        List<UserReport> reports = reportService.getMyReports(userId);
        return ResponseEntity.ok(ApiResponse.success(reports, "Your reports retrieved successfully."));
    }

    // ── Lost Items Endpoints ──────────────────────────────────────────────────

    @PostMapping("/lost-items")
    public ResponseEntity<ApiResponse<LostItem>> createLostItemReport(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateLostItemRequest req
    ) {
        LostItem item = lostItemService.createReport(
                userId,
                req.getTripId(),
                req.getItemName(),
                req.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success(item, "Lost item reported successfully."));
    }

    @GetMapping("/lost-items/my")
    public ResponseEntity<ApiResponse<List<LostItem>>> getMyLostItems(
            @AuthenticationPrincipal UUID userId
    ) {
        List<LostItem> items = lostItemService.getMyReports(userId);
        return ResponseEntity.ok(ApiResponse.success(items, "Your lost item reports retrieved successfully."));
    }
}
