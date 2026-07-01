package com.voum.modules.support.dto;

import com.voum.modules.support.entity.LostItem.LostItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostItemStatusRequest {
    @NotNull(message = "Status is required")
    private LostItemStatus status;
}
