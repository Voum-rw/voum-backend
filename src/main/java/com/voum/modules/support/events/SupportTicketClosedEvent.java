package com.voum.modules.support.events;

import com.voum.modules.support.entity.SupportTicket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SupportTicketClosedEvent {
    private final SupportTicket ticket;
}
