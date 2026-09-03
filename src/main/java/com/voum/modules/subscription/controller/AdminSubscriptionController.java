package com.voum.modules.subscription.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.subscription.dto.MoMoTransactionDto;
import com.voum.modules.subscription.dto.ReconcileTransactionRequest;
import com.voum.modules.subscription.dto.SubscriptionOverviewDto;
import com.voum.modules.subscription.entity.SubscriptionPlan;
import com.voum.modules.subscription.service.MoMoPaymentService;
import com.voum.modules.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final MoMoPaymentService moMoPaymentService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionOverviewDto>> getOverview() {
        SubscriptionOverviewDto overview = subscriptionService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview, "Subscription overview retrieved successfully."));
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionPlan>>> getPlans() {
        List<SubscriptionPlan> plans = subscriptionService.getActivePlans();
        return ResponseEntity.ok(ApiResponse.success(plans, "Subscription plans retrieved successfully."));
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> updatePlanPricing(
            @PathVariable("id") String planId,
            @RequestBody Map<String, Object> body
    ) {
        BigDecimal price = body.containsKey("priceRwf") ? new BigDecimal(body.get("priceRwf").toString()) : null;
        String desc = body.containsKey("description") ? (String) body.get("description") : null;

        SubscriptionPlan updated = subscriptionService.updatePlanPricing(planId, price, desc);
        return ResponseEntity.ok(ApiResponse.success(updated, "Plan pricing updated successfully."));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<MoMoTransactionDto>>> listTransactions(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "planId", required = false) String planId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<MoMoTransactionDto> transactions = moMoPaymentService.listTransactions(status, planId, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions, "Transactions ledger retrieved successfully."));
    }

    @PostMapping("/transactions/{id}/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MoMoTransactionDto>> manuallyReconcile(
            @PathVariable("id") UUID transactionId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody ReconcileTransactionRequest request
    ) {
        MoMoTransactionDto reconciled = moMoPaymentService.manuallyReconcile(transactionId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(reconciled, "Transaction reconciled and Motari subscription activated."));
    }
}
