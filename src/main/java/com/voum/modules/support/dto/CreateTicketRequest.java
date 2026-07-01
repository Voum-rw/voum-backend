package com.voum.modules.support.dto;

import com.voum.modules.support.entity.SupportTicket.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    @NotNull(message = "Ticket type is required")
    private TicketType type;
    private UUID tripId;
    @NotBlank(message = "Subject is required")
    private String subject;
    @NotBlank(message = "Description is required")
    private String description;
    private List<String> fileUrls;
    private List<String> fileTypes;
}
