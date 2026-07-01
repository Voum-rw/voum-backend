package com.voum.modules.admin.dto;

import com.voum.modules.users.Role;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailsResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String status;
    private Double rating;
    private Integer completedTrips;
    private Boolean isOnline;
    private Boolean isVerified;
    private Boolean isBlocked;
    private String subscriptionPlan;
    private String suspensionReason;
    private Instant suspendedAt;
    private UUID suspendedBy;
    private UUID deletedBy;
    private Integer flagCount;
    private Boolean isFlagged;
    private Instant createdAt;
    private Instant updatedAt;

    // Passenger specific fields
    private String passengerFirstName;
    private String passengerLastName;
    private String passengerProfileImage;

    // Motari specific fields
    private String motariFirstName;
    private String motariLastName;
    private String motariNationalId;
    private String motariMotoPlateNumber;
    private String motariProfileImage;
    private String motariVerificationStatus;
    private Double motariAverageRating;
    private Integer motariTotalReviews;
    private Double motariCompletionRate;
    private Double motariAcceptanceRate;
    private Double motariCancellationRate;
    private Double motariTrustScore;
}
