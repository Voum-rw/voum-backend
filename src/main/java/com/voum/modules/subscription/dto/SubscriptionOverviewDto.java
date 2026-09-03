package com.voum.modules.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionOverviewDto {
    private BigDecimal totalMrrRwf;
    private BigDecimal todayMomoIntakeRwf;
    private long activeSubscribersCount;
    private long expiringSoonCount;
    private long inGracePeriodCount;
    private long expiredCount;
    private double momoSuccessRate;
}
