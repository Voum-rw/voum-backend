package com.voum.modules.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @Column(length = 50)
    private String id; // DAILY_PASS, WEEKLY_FLEX, MONTHLY_STANDARD, QUARTERLY_PRO

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "price_rwf", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceRwf;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_popular", nullable = false)
    private Boolean isPopular = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
