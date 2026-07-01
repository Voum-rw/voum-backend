package com.voum.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyMessageRequest {
    @NotBlank(message = "Message text is required")
    private String message;
}
