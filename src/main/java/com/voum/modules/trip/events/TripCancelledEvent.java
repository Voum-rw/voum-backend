package com.voum.modules.trip.events;

import com.voum.modules.trip.entity.Trip;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TripCancelledEvent extends ApplicationEvent {

    private final Trip trip;

    public TripCancelledEvent(Object source, Trip trip) {
        super(source);
        this.trip = trip;
    }
}
