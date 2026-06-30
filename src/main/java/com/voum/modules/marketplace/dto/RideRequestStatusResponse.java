package com.voum.modules.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestStatusResponse {
    private String status;
    private Integer offersCount;
    private Long secondsRemaining;
}
