package com.voum.modules.location.mapper;

import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.users.Motari;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public NearbyMotariResponse toNearbyResponse(Motari motari, UserLocation location, double distanceKm) {
        if (motari == null || location == null) {
            return null;
        }

        return NearbyMotariResponse.builder()
                .motariId(motari.getId())
                .firstName(motari.getFirstName())
                .profileImage(motari.getProfileImage())
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0) // Round to 2 decimal places
                .availabilityStatus(location.getAvailabilityStatus())
                .verificationStatus(motari.getVerificationStatus())
                .build();
    }
}
