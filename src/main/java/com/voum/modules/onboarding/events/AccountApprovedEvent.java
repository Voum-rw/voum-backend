package com.voum.modules.onboarding.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class AccountApprovedEvent extends ApplicationEvent {

    private final UUID motariUserId;

    public AccountApprovedEvent(Object source, UUID motariUserId) {
        super(source);
        this.motariUserId = motariUserId;
    }
}
