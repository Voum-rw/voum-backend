package com.voum.modules.marketplace.events;

import com.voum.modules.marketplace.entity.RideOffer;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RideOfferSubmittedEvent extends ApplicationEvent {

    private final RideOffer rideOffer;

    public RideOfferSubmittedEvent(Object source, RideOffer rideOffer) {
        super(source);
        this.rideOffer = rideOffer;
    }
}
