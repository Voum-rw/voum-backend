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
public class RequestExpiredMessage {
    private String eventType; // "REQUEST_EXPIRED"
    private int eventVersion;
    private long sequence;
    
    private UUID requestId;
}
