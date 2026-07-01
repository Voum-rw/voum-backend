package com.voum.modules.support.events;

import com.voum.modules.support.entity.SupportTicket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class SupportTicketAssignedEvent {
    private final SupportTicket ticket;
    private final UUID adminId;
}
