package com.voum.modules.review.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotariReviewResponse {
    private UUID id;
    private UUID tripId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
