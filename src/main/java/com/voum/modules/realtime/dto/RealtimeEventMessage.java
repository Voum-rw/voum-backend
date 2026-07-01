package com.voum.modules.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventType;
    @Builder.Default
    private int eventVersion = 1;
    private long sequence;
    private List<UUID> driverIds; // for Motari targeted broadcasts
    private UUID requestId;       // for passenger broadcast channel
    private UUID tripId;          // for trip broadcast channel
    private Object payload;       // actual event payload (e.g. RequestCreatedMessage, OfferCreatedMessage)
}
