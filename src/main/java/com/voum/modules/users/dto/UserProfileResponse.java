package com.voum.modules.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String role;
    private Double rating;
    private Integer completedTrips;
    private Boolean isOnline;
    private Boolean isVerified;
    private String subscriptionPlan;
    private Instant createdAt;

    // Optional Passenger Details
    private PassengerDetails passenger;

    // Optional Motari Details
    private MotariDetails motari;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDetails {
        private String firstName;
        private String lastName;
        private String profileImage;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotariDetails {
        private String firstName;
        private String lastName;
        private String nationalId;
        private String motoPlateNumber;
        private String motoModel;
        private String motoColor;
        private String profileImage;
        private String verificationStatus;
        private String status;
        private Double averageRating;
        private Integer totalCompletedTrips;
        private Double acceptanceRate;
        private Double completionRate;
        private Instant createdAt;
    }
}
