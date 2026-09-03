package com.voum.modules.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoMoTransactionDto {
    private UUID id;
    private String momoTransactionId;
    private String externalReference;
    private String financialTransactionId;
    private UUID motariId;
    private String motariName;
    private String phoneNumber;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String planId;
    private String planName;
    private String paymentGateway;
    private String payerNote;
    private String failureReason;
    private String rawGatewayResponse;
    private String reconciledByName;
    private Instant reconciledAt;
    private Instant completedAt;
    private Instant createdAt;
}
