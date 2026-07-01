package com.voum.modules.review.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotariRatingResponse {
    private Double averageRating;
    private Integer totalReviews;
    private Double completionRate;
    private Double trustScore;
}
