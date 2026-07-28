package com.voum.modules.onboarding;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "motari_verification_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotariVerificationProfile {

    @Id
    @Column(name = "motari_id", nullable = false)
    private UUID motariId;

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "national_id_number")
    private String nationalIdNumber;

    @Column(name = "permit_number")
    private String permitNumber;

    @Column(name = "permit_expiry_date")
    private LocalDate permitExpiryDate;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Builder.Default
    @Column(name = "verification_level")
    private String verificationLevel = "LEVEL_0"; // LEVEL_0, LEVEL_1, LEVEL_2, LEVEL_3

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
