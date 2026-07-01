package com.voum.modules.tracking.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

@Getter
public class TripLocationUpdatedEvent extends ApplicationEvent {

    private final UUID tripId;
    private final Double latitude;
    private final Double longitude;
    private final Double speedKmh;
    private final Instant recordedAt;

    public TripLocationUpdatedEvent(Object source, UUID tripId, Double latitude, Double longitude, Double speedKmh, Instant recordedAt) {
        super(source);
        this.tripId = tripId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = speedKmh;
        this.recordedAt = recordedAt;
    }
}
