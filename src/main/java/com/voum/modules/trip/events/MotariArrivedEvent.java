package com.voum.modules.trip.events;

import com.voum.modules.trip.entity.Trip;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MotariArrivedEvent extends ApplicationEvent {

    private final Trip trip;

    public MotariArrivedEvent(Object source, Trip trip) {
        super(source);
        this.trip = trip;
    }
}
