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
public class OfferWithdrawnMessage {
    private String eventType; // "OFFER_WITHDRAWN"
    private int eventVersion;
    private long sequence;
    
    private UUID offerId;
    private UUID requestId;
    private UUID motariId;
    private Integer offersCount;
}
