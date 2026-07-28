package com.voum.modules.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyActivityResponse {
    private String zoneName;
    private String demandLevel; // "High", "Medium", "Low"
    private String subtitle;    // e.g. "Wait < 2m · +5 active requests"
    private boolean isHighDemand;
    private int activeRequestsCount;
}
