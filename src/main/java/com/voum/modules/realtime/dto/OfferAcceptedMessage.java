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
public class OfferAcceptedMessage {
    private String eventType; // "OFFER_ACCEPTED"
    private int eventVersion;
    private long sequence;
    
    private UUID requestId;
    private UUID offerId;
    private UUID motariId;
}
