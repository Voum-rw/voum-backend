package com.voum.modules.subscription.service;

import com.voum.common.ApiException;
import com.voum.modules.subscription.dto.MoMoTransactionDto;
import com.voum.modules.subscription.dto.ReconcileTransactionRequest;
import com.voum.modules.subscription.entity.MoMoTransaction;
import com.voum.modules.subscription.entity.MotariSubscription;
import com.voum.modules.subscription.entity.SubscriptionPlan;
import com.voum.modules.subscription.repository.MoMoTransactionRepository;
import com.voum.modules.subscription.repository.SubscriptionPlanRepository;
import com.voum.modules.users.Motari;
import com.voum.modules.users.MotariRepository;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import com.voum.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoPaymentService {

    private final MoMoTransactionRepository transactionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final MotariRepository motariRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final AuditLogService auditLogService;

    public Page<MoMoTransactionDto> listTransactions(String status, String planId, String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        String normStatus = (status != null && !status.equalsIgnoreCase("ALL")) ? status : null;
        String normPlan = (planId != null && !planId.equalsIgnoreCase("ALL")) ? planId : null;
        String normQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<MoMoTransaction> txPage = transactionRepository.searchTransactions(normStatus, normPlan, normQuery, pageRequest);
        return txPage.map(this::mapToDto);
    }

    @Transactional
    public MoMoTransactionDto handleInboundMoMoCallback(
            String momoTxId,
            String externalRef,
            String phone,
            BigDecimal amount,
            String planId,
            String status,
            String financialTxId,
            String rawPayload
    ) {
        // Find or associate motari by phone number
        Motari motari = motariRepository.findByPhoneNumber(phone)
                .orElseGet(() -> motariRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ApiException("No registered Motari found for phone " + phone, HttpStatus.NOT_FOUND)));

        SubscriptionPlan plan = planRepository.findById(planId != null ? planId : "MONTHLY_STANDARD")
                .orElseGet(() -> planRepository.findById("MONTHLY_STANDARD").orElse(null));

        MoMoTransaction transaction = transactionRepository.findByMomoTransactionId(momoTxId)
                .orElseGet(() -> MoMoTransaction.builder()
                        .momoTransactionId(momoTxId)
                        .externalReference(externalRef != null ? externalRef : "VOUM-" + System.currentTimeMillis())
                        .build());

        transaction.setMotari(motari);
        transaction.setPlan(plan);
        transaction.setPhoneNumber(phone);
        transaction.setAmount(amount != null ? amount : (plan != null ? plan.getPriceRwf() : BigDecimal.valueOf(15000)));
        transaction.setStatus(status != null ? status : "SUCCESSFUL");
        transaction.setFinancialTransactionId(financialTxId);
        transaction.setRawGatewayResponse(rawPayload);
        transaction.setCompletedAt(Instant.now());

        if ("SUCCESSFUL".equalsIgnoreCase(status) && plan != null) {
            MotariSubscription sub = subscriptionService.activateOrRenewSubscription(motari.getId(), plan.getId(), "MTN_MOMO");
            transaction.setSubscription(sub);
        }

        MoMoTransaction saved = transactionRepository.save(transaction);
        log.info("Processed MoMo Callback: Tx {} Status {} Amount {}", momoTxId, status, amount);
        return mapToDto(saved);
    }

    @Transactional
    public MoMoTransactionDto manuallyReconcile(UUID transactionId, ReconcileTransactionRequest request, UUID adminId) {
        MoMoTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException("Transaction not found: " + transactionId, HttpStatus.NOT_FOUND));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin not found: " + adminId, HttpStatus.NOT_FOUND));

        tx.setStatus("SUCCESSFUL");
        tx.setFinancialTransactionId(request.getFinancialTransactionId());
        tx.setReconciledBy(admin);
        tx.setReconciledAt(Instant.now());
        tx.setCompletedAt(Instant.now());

        if (tx.getPlan() != null && tx.getMotari() != null) {
            MotariSubscription sub = subscriptionService.activateOrRenewSubscription(tx.getMotari().getId(), tx.getPlan().getId(), "MANUAL_RECONCILIATION");
            tx.setSubscription(sub);
        }

        MoMoTransaction saved = transactionRepository.save(tx);

        auditLogService.logAction(
                adminId,
                "MOMO_MANUAL_RECONCILE",
                "Transaction: " + tx.getMomoTransactionId(),
                "FINANCIAL_LEDGER",
                Map.of(
                    "financialRef", request.getFinancialTransactionId(),
                    "verifiedBy", admin.getName()
                )
        );

        return mapToDto(saved);
    }

    private MoMoTransactionDto mapToDto(MoMoTransaction tx) {
        String driverName = tx.getMotari() != null ? 
                tx.getMotari().getFirstName() + " " + tx.getMotari().getLastName() : "Unknown Motari";

        return MoMoTransactionDto.builder()
                .id(tx.getId())
                .momoTransactionId(tx.getMomoTransactionId())
                .externalReference(tx.getExternalReference())
                .financialTransactionId(tx.getFinancialTransactionId())
                .motariId(tx.getMotari() != null ? tx.getMotari().getId() : null)
                .motariName(driverName)
                .phoneNumber(tx.getPhoneNumber())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .planId(tx.getPlan() != null ? tx.getPlan().getId() : null)
                .planName(tx.getPlan() != null ? tx.getPlan().getName() : "Subscription Plan")
                .paymentGateway(tx.getPaymentGateway())
                .payerNote(tx.getPayerNote())
                .failureReason(tx.getFailureReason())
                .rawGatewayResponse(tx.getRawGatewayResponse())
                .reconciledByName(tx.getReconciledBy() != null ? tx.getReconciledBy().getName() : null)
                .reconciledAt(tx.getReconciledAt())
                .completedAt(tx.getCompletedAt())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
