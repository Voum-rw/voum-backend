package com.voum.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostItemRequest {
    private UUID tripId;
    @NotBlank(message = "Item name is required")
    private String itemName;
    @NotBlank(message = "Description is required")
    private String description;
}
