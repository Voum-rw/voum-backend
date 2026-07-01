package com.voum.modules.trip.events;

import com.voum.modules.trip.entity.Trip;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PassengerBoardedEvent extends ApplicationEvent {

    private final Trip trip;

    public PassengerBoardedEvent(Object source, Trip trip) {
        super(source);
        this.trip = trip;
    }
}
