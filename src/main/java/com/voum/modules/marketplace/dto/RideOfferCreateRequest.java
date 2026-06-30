package com.voum.modules.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferCreateRequest {

    @NotNull(message = "Ride request ID is required")
    private UUID rideRequestId;

    @NotNull(message = "Offered price is required")
    @DecimalMin(value = "1.0", message = "Price must be greater than or equal to 1.0")
    private Double offeredPrice;

    @NotNull(message = "Estimated arrival minutes is required")
    @Min(value = 1, message = "Estimated arrival must be at least 1 minute")
    private Integer estimatedArrivalMinutes;
}
