package com.voum.modules.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyMotariResponse {
    private UUID motariId;
    private String firstName;
    private String profileImage;
    private Double distanceKm;
    private String availabilityStatus;
    private String verificationStatus;
}
