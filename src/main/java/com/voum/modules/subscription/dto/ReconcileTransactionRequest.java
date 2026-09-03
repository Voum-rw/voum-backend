package com.voum.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReconcileTransactionRequest {
    @NotBlank(message = "Financial reference ID is required")
    private String financialTransactionId;

    private String note;
}
