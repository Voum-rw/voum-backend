package com.voum.modules.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripEtaResponse {
    private Integer estimatedArrivalMinutes;
}
