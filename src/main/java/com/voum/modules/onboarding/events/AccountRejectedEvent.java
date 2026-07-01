package com.voum.modules.onboarding.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class AccountRejectedEvent extends ApplicationEvent {

    private final UUID motariUserId;
    private final String rejectionReason;

    public AccountRejectedEvent(Object source, UUID motariUserId, String rejectionReason) {
        super(source);
        this.motariUserId = motariUserId;
        this.rejectionReason = rejectionReason;
    }
}
