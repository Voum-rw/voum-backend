package com.voum.modules.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferUpdatedMessage {
    private String eventType; // "OFFER_UPDATED"
    private int eventVersion;
    private long sequence;
    
    private UUID offerId;
    private UUID requestId;
    private UUID motariId;
    private Double price;
    private Integer estimatedArrivalMinutes;
    private Integer offersCount;
}
