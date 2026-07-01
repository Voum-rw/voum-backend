package com.voum.modules.support.service;

import com.voum.common.ApiException;
import com.voum.modules.users.Role;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import com.voum.modules.support.entity.SupportTicket;
import com.voum.modules.support.entity.TicketAttachment;
import com.voum.modules.support.entity.TicketMessage;
import com.voum.modules.support.repository.SupportTicketRepository;
import com.voum.modules.support.repository.TicketAttachmentRepository;
import com.voum.modules.support.repository.TicketMessageRepository;
import com.voum.modules.support.events.SupportTicketCreatedEvent;
import com.voum.modules.support.events.SupportTicketAssignedEvent;
import com.voum.modules.support.events.SupportTicketClosedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SupportTicket createTicket(UUID userId, SupportTicket.TicketType type, UUID tripId, String subject, String description, List<String> fileUrls, List<String> fileTypes) {
        SupportTicket.TicketPriority priority = (type == SupportTicket.TicketType.SAFETY_EMERGENCY) ?
                SupportTicket.TicketPriority.URGENT : SupportTicket.TicketPriority.MEDIUM;

        SupportTicket ticket = SupportTicket.builder()
                .userId(userId)
                .tripId(tripId)
                .type(type)
                .subject(subject)
                .description(description)
                .status(SupportTicket.TicketStatus.OPEN)
                .priority(priority)
                .build();

        ticket = supportTicketRepository.save(ticket);

        if (fileUrls != null && fileTypes != null) {
            int size = Math.min(fileUrls.size(), fileTypes.size());
            for (int i = 0; i < size; i++) {
                TicketAttachment attachment = TicketAttachment.builder()
                        .ticketId(ticket.getId())
                        .fileUrl(fileUrls.get(i))
                        .fileType(fileTypes.get(i))
                        .build();
                ticketAttachmentRepository.save(attachment);
            }
        }

        // Add the description as the first message thread item
        TicketMessage firstMsg = TicketMessage.builder()
                .ticketId(ticket.getId())
                .senderId(userId)
                .message(description)
                .build();
        ticketMessageRepository.save(firstMsg);

        eventPublisher.publishEvent(new SupportTicketCreatedEvent(ticket));

        return ticket;
    }

    @Transactional
    public TicketMessage replyToTicket(UUID ticketId, UUID senderId, String message) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));

        if (ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            throw new ApiException("Ticket is closed. Please reopen first.", HttpStatus.BAD_REQUEST);
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ApiException("Sender not found.", HttpStatus.NOT_FOUND));

        // Ownership/Role Authorization
        boolean isAdmin = (sender.getRole() == Role.ADMIN || sender.getRole() == Role.SUPER_ADMIN);
        if (!isAdmin && !ticket.getUserId().equals(senderId)) {
            throw new ApiException("You do not own this ticket.", HttpStatus.FORBIDDEN);
        }

        TicketMessage msg = TicketMessage.builder()
                .ticketId(ticketId)
                .senderId(senderId)
                .message(message)
                .build();
        msg = ticketMessageRepository.save(msg);

        // SLA: If admin is replying and firstResponseAt is null, track first response
        if (isAdmin && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(Instant.now());
            supportTicketRepository.save(ticket);
        }

        return msg;
    }

    @Transactional
    public void assignTicket(UUID ticketId, UUID adminId, UUID actorAdminId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));

        if (ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            throw new ApiException("Cannot assign a closed ticket.", HttpStatus.BAD_REQUEST);
        }

        ticket.setAssignedAdminId(adminId);
        if (ticket.getStatus() == SupportTicket.TicketStatus.OPEN) {
            ticket.setStatus(SupportTicket.TicketStatus.ASSIGNED);
        }
        supportTicketRepository.save(ticket);

        eventPublisher.publishEvent(new SupportTicketAssignedEvent(ticket, adminId));
    }

    @Transactional
    public void closeTicket(UUID ticketId, UUID adminId, String resolutionSummary) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));

        ticket.setStatus(SupportTicket.TicketStatus.CLOSED);
        ticket.setResolvedBy(adminId);
        ticket.setResolvedAt(Instant.now());
        ticket.setResolutionSummary(resolutionSummary);
        supportTicketRepository.save(ticket);

        eventPublisher.publishEvent(new SupportTicketClosedEvent(ticket));
    }

    @Transactional
    public void reopenTicket(UUID ticketId, UUID userId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        boolean isAdmin = (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN);
        if (!isAdmin && !ticket.getUserId().equals(userId)) {
            throw new ApiException("You do not own this ticket.", HttpStatus.FORBIDDEN);
        }

        ticket.setStatus(SupportTicket.TicketStatus.REOPENED);
        ticket.setResolvedAt(null);
        ticket.setResolvedBy(null);
        ticket.setResolutionSummary(null);
        supportTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicketDetails(UUID ticketId, UUID userId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        boolean isAdmin = (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN);
        if (!isAdmin && !ticket.getUserId().equals(userId)) {
            throw new ApiException("You do not own this ticket.", HttpStatus.FORBIDDEN);
        }

        return ticket;
    }

    @Transactional(readOnly = true)
    public List<TicketMessage> getTicketMessages(UUID ticketId, UUID userId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        boolean isAdmin = (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN);
        if (!isAdmin && !ticket.getUserId().equals(userId)) {
            throw new ApiException("You do not own this ticket.", HttpStatus.FORBIDDEN);
        }

        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional(readOnly = true)
    public List<TicketAttachment> getTicketAttachments(UUID ticketId) {
        return ticketAttachmentRepository.findByTicketId(ticketId);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getUserTickets(UUID userId) {
        return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    @Transactional
    public void updateInternalNotes(UUID ticketId, String notes) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found.", HttpStatus.NOT_FOUND));
        ticket.setInternalNotes(notes);
        supportTicketRepository.save(ticket);
    }
}
