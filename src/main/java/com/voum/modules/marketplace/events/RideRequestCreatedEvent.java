package com.voum.modules.marketplace.events;

import com.voum.modules.marketplace.entity.RideRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RideRequestCreatedEvent extends ApplicationEvent {
    
    private final RideRequest rideRequest;

    public RideRequestCreatedEvent(Object source, RideRequest rideRequest) {
        super(source);
        this.rideRequest = rideRequest;
    }
}
