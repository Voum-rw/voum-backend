package com.voum.modules.review.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class TrustScoreUpdatedEvent extends ApplicationEvent {

    private final UUID motariId;
    private final double oldScore;
    private final double newScore;

    public TrustScoreUpdatedEvent(Object source, UUID motariId, double oldScore, double newScore) {
        super(source);
        this.motariId = motariId;
        this.oldScore = oldScore;
        this.newScore = newScore;
    }
}
