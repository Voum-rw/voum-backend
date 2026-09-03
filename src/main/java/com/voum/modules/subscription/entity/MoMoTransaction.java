package com.voum.modules.subscription.entity;

import com.voum.modules.users.Motari;
import com.voum.modules.users.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "momo_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoMoTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "momo_transaction_id", nullable = false, unique = true, length = 100)
    private String momoTransactionId;

    @Column(name = "external_reference", nullable = false, unique = true, length = 150)
    private String externalReference;

    @Column(name = "financial_transaction_id", length = 100)
    private String financialTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motari_id", nullable = false)
    private Motari motari;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private MotariSubscription subscription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(length = 10, nullable = false)
    private String currency = "RWF";

    @Builder.Default
    @Column(length = 30, nullable = false)
    private String status = "PENDING"; // PENDING, SUCCESSFUL, FAILED, REFUNDED

    @Builder.Default
    @Column(name = "payment_gateway", length = 50, nullable = false)
    private String paymentGateway = "MTN_MOMO";

    @Column(name = "payer_note", length = 255)
    private String payerNote;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "raw_gateway_response", columnDefinition = "TEXT")
    private String rawGatewayResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciled_by")
    private User reconciledBy;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
