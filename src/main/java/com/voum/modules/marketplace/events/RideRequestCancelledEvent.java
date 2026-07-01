package com.voum.modules.marketplace.events;

import com.voum.modules.marketplace.entity.RideRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RideRequestCancelledEvent extends ApplicationEvent {

    private final RideRequest rideRequest;

    public RideRequestCancelledEvent(Object source, RideRequest rideRequest) {
        super(source);
        this.rideRequest = rideRequest;
    }
}
