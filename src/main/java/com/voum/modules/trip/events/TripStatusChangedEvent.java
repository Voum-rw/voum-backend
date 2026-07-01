package com.voum.modules.trip.events;

import com.voum.modules.trip.entity.Trip;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TripStatusChangedEvent extends ApplicationEvent {

    private final Trip trip;
    private final String previousStatus;
    private final String newStatus;

    public TripStatusChangedEvent(Object source, Trip trip, String previousStatus, String newStatus) {
        super(source);
        this.trip = trip;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }
}
