package com.voum.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelMetricsResponse {
    private long requestsCreated;
    private long requestsWithOffers;
    private long requestsAccepted;
    private long tripsStarted;
    private long tripsCompleted;

    // Conversion Rates
    private double requestToOfferRate;
    private double offerToAcceptRate;
    private double acceptToStartRate;
    private double startToCompleteRate;
    private double overallFunnelConversionRate;
}
