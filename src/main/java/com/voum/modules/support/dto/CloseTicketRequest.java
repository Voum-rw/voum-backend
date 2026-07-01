package com.voum.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseTicketRequest {
    @NotBlank(message = "Resolution summary is required")
    private String resolutionSummary;
}
