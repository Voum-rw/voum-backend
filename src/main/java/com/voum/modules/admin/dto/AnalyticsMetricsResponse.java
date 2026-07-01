package com.voum.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMetricsResponse {
    private String period; // DAILY, WEEKLY, MONTHLY
    private long newUsers;
    private long newMotaris;
    private long totalTrips;
    private long completedTrips;
    private long cancelledTrips;
    private long offersSubmitted;
    private double completionRate;
    private double cancellationRate;
}
