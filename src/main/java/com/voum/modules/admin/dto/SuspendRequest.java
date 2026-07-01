package com.voum.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendRequest {
    @NotBlank(message = "Suspension reason is required")
    private String reason;
}
