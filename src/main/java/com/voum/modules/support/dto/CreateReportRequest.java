package com.voum.modules.support.dto;

import com.voum.modules.support.entity.UserReport.ReportSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {
    @NotNull(message = "Reported user ID is required")
    private UUID reportedUserId;
    private UUID tripId;
    @NotBlank(message = "Reason is required")
    private String reason;
    @NotBlank(message = "Description is required")
    private String description;
    @NotNull(message = "Severity is required")
    private ReportSeverity severity;
}
