package com.voum.modules.marketplace.events;

import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RideOfferAcceptedEvent extends ApplicationEvent {

    private final RideRequest rideRequest;
    private final RideOffer rideOffer;

    public RideOfferAcceptedEvent(Object source, RideRequest rideRequest, RideOffer rideOffer) {
        super(source);
        this.rideRequest = rideRequest;
        this.rideOffer = rideOffer;
    }
}
