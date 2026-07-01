package com.voum.modules.support.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTicketRequest {
    @NotNull(message = "Assigned admin ID is required")
    private UUID assignedAdminId;
}
