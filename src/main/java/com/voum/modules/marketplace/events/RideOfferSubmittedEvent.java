package com.voum.modules.marketplace.events;

import com.voum.modules.marketplace.entity.RideOffer;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class RideOfferSubmittedEvent extends ApplicationEvent {

    private final RideOffer rideOffer;
    private final UUID passengerId;

    public RideOfferSubmittedEvent(Object source, RideOffer rideOffer, UUID passengerId) {
        super(source);
        this.rideOffer = rideOffer;
        this.passengerId = passengerId;
    }
}

