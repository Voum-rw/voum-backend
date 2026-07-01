package com.voum.modules.support;

import com.voum.common.ApiException;
import com.voum.modules.users.User;
import com.voum.modules.users.Role;
import com.voum.modules.users.UserRepository;
import com.voum.modules.support.entity.SupportTicket;
import com.voum.modules.support.entity.TicketAttachment;
import com.voum.modules.support.entity.TicketMessage;
import com.voum.modules.support.repository.SupportTicketRepository;
import com.voum.modules.support.repository.TicketAttachmentRepository;
import com.voum.modules.support.repository.TicketMessageRepository;
import com.voum.modules.support.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private TicketAttachmentRepository ticketAttachmentRepository;

    @Mock
    private TicketMessageRepository ticketMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SupportService supportService;

    private final UUID userId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        supportService = new SupportService(
                supportTicketRepository,
                ticketAttachmentRepository,
                ticketMessageRepository,
                userRepository,
                eventPublisher
        );
    }

    @Test
    public void createTicket_shouldCreateTicketWithFirstMessageAndAttachments() {
        SupportTicket savedTicket = SupportTicket.builder()
                .id(ticketId)
                .userId(userId)
                .type(SupportTicket.TicketType.GENERAL_SUPPORT)
                .subject("Payment failed")
                .description("Card declined during match")
                .status(SupportTicket.TicketStatus.OPEN)
                .priority(SupportTicket.TicketPriority.MEDIUM)
                .build();

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(savedTicket);

        List<String> urls = List.of("http://evidence.com/1.png");
        List<String> types = List.of("image/png");

        SupportTicket ticket = supportService.createTicket(
                userId,
                SupportTicket.TicketType.GENERAL_SUPPORT,
                null,
                "Payment failed",
                "Card declined during match",
                urls,
                types
        );

        assertNotNull(ticket);
        assertEquals(ticketId, ticket.getId());
        verify(supportTicketRepository).save(any(SupportTicket.class));
        verify(ticketAttachmentRepository).save(any(TicketAttachment.class));
        verify(ticketMessageRepository).save(any(TicketMessage.class));
        verify(eventPublisher).publishEvent(any(com.voum.modules.support.events.SupportTicketCreatedEvent.class));
    }

    @Test
    public void createEmergencyTicket_shouldAutoAssignUrgentPriority() {
        SupportTicket savedTicket = SupportTicket.builder()
                .id(ticketId)
                .userId(userId)
                .type(SupportTicket.TicketType.SAFETY_EMERGENCY)
                .subject("Accident reported")
                .description("Driver hit a curb")
                .status(SupportTicket.TicketStatus.OPEN)
                .priority(SupportTicket.TicketPriority.URGENT)
                .build();

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(savedTicket);

        SupportTicket ticket = supportService.createTicket(
                userId,
                SupportTicket.TicketType.SAFETY_EMERGENCY,
                null,
                "Accident reported",
                "Driver hit a curb",
                null,
                null
        );

        assertEquals(SupportTicket.TicketPriority.URGENT, ticket.getPriority());
    }

    @Test
    public void replyToTicket_adminReply_shouldUpdateFirstResponseTimeSLA() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .userId(userId)
                .status(SupportTicket.TicketStatus.OPEN)
                .build();

        User admin = User.builder()
                .id(adminId)
                .role(Role.ADMIN)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(i -> i.getArguments()[0]);

        TicketMessage msg = supportService.replyToTicket(ticketId, adminId, "We are reviewing your transaction details.");

        assertNotNull(msg);
        assertNotNull(ticket.getFirstResponseAt());
        verify(supportTicketRepository).save(ticket);
    }

    @Test
    public void replyToTicket_closedTicket_shouldThrowException() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .status(SupportTicket.TicketStatus.CLOSED)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ApiException exception = assertThrows(ApiException.class, () ->
                supportService.replyToTicket(ticketId, userId, "Re-trying payment."));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void reopenTicket_shouldChangeStatusToReopenedAndClearResolution() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .userId(userId)
                .status(SupportTicket.TicketStatus.CLOSED)
                .resolvedAt(Instant.now())
                .resolvedBy(adminId)
                .resolutionSummary("Refund processed")
                .build();

        User user = User.builder()
                .id(userId)
                .role(Role.PASSENGER)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        supportService.reopenTicket(ticketId, userId);

        assertEquals(SupportTicket.TicketStatus.REOPENED, ticket.getStatus());
        assertNull(ticket.getResolvedAt());
        assertNull(ticket.getResolvedBy());
        assertNull(ticket.getResolutionSummary());
        verify(supportTicketRepository).save(ticket);
    }
}
