package com.voum.modules.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStatsResponse {
    private long todayCompletedCount;
    private long weeklyCompletedCount;
    private long monthlyCompletedCount;
    private long totalCompletedCount;
    private long totalCancelledCount;
    private Map<String, Long> dailyPerformance; // Mon: 0, Tue: 2, etc.
}
