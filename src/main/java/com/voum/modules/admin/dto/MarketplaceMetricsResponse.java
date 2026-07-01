package com.voum.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceMetricsResponse {
    private double matchingSuccessRate;
    private double averageOfferResponseTimeSeconds;
    private long openRequestsCount;
    private long activeOffersCount;
}
