package com.voum.modules.trip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCancelRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
