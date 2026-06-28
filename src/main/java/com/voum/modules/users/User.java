package com.voum.modules.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Column(unique = true, nullable = false)
    private String phone;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private Double rating = 5.0;

    @Builder.Default
    @Column(name = "completed_trips", nullable = false)
    private Integer completedTrips = 0;

    @Builder.Default
    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Builder.Default
    @Column(name = "current_lat", nullable = false)
    private Double currentLat = -1.9441; // Default Kigali Center

    @Builder.Default
    @Column(name = "current_lng", nullable = false)
    private Double currentLng = 30.0619; // Default Kigali Center

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Builder.Default
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    @Builder.Default
    @Column(name = "subscription_plan", nullable = false)
    private String subscriptionPlan = "Free";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
