package com.voum.modules.support;

import com.voum.common.ApiException;
import com.voum.modules.users.User;
import com.voum.modules.users.Role;
import com.voum.modules.users.UserRepository;
import com.voum.modules.support.entity.SupportTicket;
import com.voum.modules.support.repository.SupportTicketRepository;
import com.voum.modules.support.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminSupportServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SupportService supportService;

    private final UUID ticketId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        supportService = new SupportService(
                supportTicketRepository,
                null,
                null,
                userRepository,
                eventPublisher
        );
    }

    @Test
    public void getTicketDetails_adminRequester_shouldBypassOwnershipCheck() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .userId(requesterId)
                .build();

        User admin = User.builder()
                .id(adminId)
                .role(Role.ADMIN)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        SupportTicket result = supportService.getTicketDetails(ticketId, adminId);

        assertNotNull(result);
        assertEquals(ticketId, result.getId());
    }

    @Test
    public void getTicketDetails_differentUserRequester_shouldThrowForbidden() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .userId(requesterId)
                .build();

        User randomUser = User.builder()
                .id(adminId)
                .role(Role.PASSENGER)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(randomUser));

        ApiException exception = assertThrows(ApiException.class, () ->
                supportService.getTicketDetails(ticketId, adminId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    public void assignTicket_shouldSetAssignedAdminAndStatus() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .status(SupportTicket.TicketStatus.OPEN)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        supportService.assignTicket(ticketId, adminId, UUID.randomUUID());

        assertEquals(adminId, ticket.getAssignedAdminId());
        assertEquals(SupportTicket.TicketStatus.ASSIGNED, ticket.getStatus());
        verify(supportTicketRepository).save(ticket);
        verify(eventPublisher).publishEvent(any(com.voum.modules.support.events.SupportTicketAssignedEvent.class));
    }

    @Test
    public void closeTicket_shouldSetClosedStatusAndSummary() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .status(SupportTicket.TicketStatus.ASSIGNED)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        supportService.closeTicket(ticketId, adminId, "Issue was investigated and resolved.");

        assertEquals(SupportTicket.TicketStatus.CLOSED, ticket.getStatus());
        assertEquals(adminId, ticket.getResolvedBy());
        assertEquals("Issue was investigated and resolved.", ticket.getResolutionSummary());
        assertNotNull(ticket.getResolvedAt());
        verify(supportTicketRepository).save(ticket);
        verify(eventPublisher).publishEvent(any(com.voum.modules.support.events.SupportTicketClosedEvent.class));
    }

    @Test
    public void updateInternalNotes_shouldSaveNotes() {
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .build();

        when(supportTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        supportService.updateInternalNotes(ticketId, "Requires further logs review.");

        assertEquals("Requires further logs review.", ticket.getInternalNotes());
        verify(supportTicketRepository).save(ticket);
    }
}
