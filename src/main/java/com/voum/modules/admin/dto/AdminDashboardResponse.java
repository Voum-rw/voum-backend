package com.voum.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalPassengers;
    private long totalMotaris;
    private long verifiedMotaris;
    private long pendingVerifications;
    private long activeTrips;
    private long completedTrips;
    private long cancelledTrips;
    private long openRideRequests;
    private long activeOnlineMotaris;

    // Driver Verification KPIs
    private long approvedTodayCount;
    private long rejectedTodayCount;
    private double approvalRate;
    private double averageVerificationTimeSeconds;

    // Safety, Disputes & Support Center metrics
    private long openTickets;
    private long resolvedTickets;
    private double averageFirstResponseTimeSeconds;
    private double averageResolutionTimeSeconds;
    private long safetyIncidentsCount;
    private long activeReportsCount;
    private long lostItemReportsCount;
}
