package com.voum.modules.subscription.entity;

import com.voum.modules.users.Motari;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "motari_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotariSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motari_id", nullable = false)
    private Motari motari;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, EXPIRING_SOON, GRACE_PERIOD, EXPIRED, CANCELLED

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "grace_period_end_date")
    private Instant gracePeriodEndDate;

    @Builder.Default
    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod = "MTN_MOMO";

    @Builder.Default
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
