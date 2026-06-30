package com.voum.modules.marketplace.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestCreateRequest {

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90.0 and 90.0")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180.0 and 180.0")
    private Double pickupLongitude;

    @NotNull(message = "Destination latitude is required")
    @DecimalMin(value = "-90.0", message = "Destination latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Destination latitude must be between -90.0 and 90.0")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    @DecimalMin(value = "-180.0", message = "Destination longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Destination longitude must be between -180.0 and 180.0")
    private Double destinationLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @NotNull(message = "Proposed budget is required")
    @DecimalMin(value = "1.0", message = "Budget must be greater than or equal to 1.0")
    private Double proposedBudget;

    @Builder.Default
    @DecimalMin(value = "0.1", message = "Visibility radius must be at least 0.1 km")
    @DecimalMax(value = "50.0", message = "Visibility radius cannot exceed 50.0 km")
    private Double visibilityRadiusKm = 3.00;

    private String createdArea;
}
