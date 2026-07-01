package com.voum.modules.review.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.review.dto.MotariRatingResponse;
import com.voum.modules.review.dto.MotariReviewResponse;
import com.voum.modules.review.dto.ReviewRequest;
import com.voum.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews/passenger")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Void>> submitPassengerReview(
            @AuthenticationPrincipal UUID passengerId,
            @Valid @RequestBody ReviewRequest request
    ) {
        reviewService.submitPassengerReview(passengerId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Review submitted successfully."));
    }

    @PostMapping("/reviews/motari")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<Void>> submitMotariReview(
            @AuthenticationPrincipal UUID motariId,
            @Valid @RequestBody ReviewRequest request
    ) {
        reviewService.submitMotariReview(motariId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Review submitted successfully."));
    }

    @GetMapping("/motaris/{id}/rating")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI', 'ADMIN')")
    public ResponseEntity<ApiResponse<MotariRatingResponse>> getMotariRating(
            @PathVariable("id") UUID motariId
    ) {
        MotariRatingResponse rating = reviewService.getMotariRating(motariId);
        return ResponseEntity.ok(ApiResponse.success(rating, "Motari rating statistics retrieved successfully."));
    }

    @GetMapping("/motaris/{id}/reviews")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<MotariReviewResponse>>> getMotariReviews(
            @PathVariable("id") UUID motariId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<MotariReviewResponse> reviews = reviewService.getMotariReviews(motariId, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews, "Motari review history retrieved successfully."));
    }
}
