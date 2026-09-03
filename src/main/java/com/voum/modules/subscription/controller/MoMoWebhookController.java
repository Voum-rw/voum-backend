package com.voum.modules.subscription.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.subscription.dto.MoMoTransactionDto;
import com.voum.modules.subscription.service.MoMoPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/momo")
@RequiredArgsConstructor
@Slf4j
public class MoMoWebhookController {

    private final MoMoPaymentService moMoPaymentService;

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<MoMoTransactionDto>> handleCallback(
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Received MTN MoMo Inbound Webhook Callback: {}", payload);

        String momoTxId = (String) payload.getOrDefault("momoTransactionId", "MTN-RW-" + System.currentTimeMillis());
        String externalRef = (String) payload.getOrDefault("externalReference", "EXT-" + System.currentTimeMillis());
        String phone = (String) payload.getOrDefault("phoneNumber", "+250 788 112 233");
        String status = (String) payload.getOrDefault("status", "SUCCESSFUL");
        String planId = (String) payload.getOrDefault("planId", "MONTHLY_STANDARD");
        String financialTxId = (String) payload.getOrDefault("financialTransactionId", "FN-" + System.currentTimeMillis() + "-MTN");

        BigDecimal amount = payload.containsKey("amount") ? 
                new BigDecimal(payload.get("amount").toString()) : BigDecimal.valueOf(15000);

        MoMoTransactionDto result = moMoPaymentService.handleInboundMoMoCallback(
                momoTxId,
                externalRef,
                phone,
                amount,
                planId,
                status,
                financialTxId,
                payload.toString()
        );

        return ResponseEntity.ok(ApiResponse.success(result, "MoMo payment webhook processed successfully."));
    }
}
