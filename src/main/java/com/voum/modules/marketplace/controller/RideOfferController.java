package com.voum.modules.marketplace.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.marketplace.dto.RideOfferCreateRequest;
import com.voum.modules.marketplace.dto.RideOfferResponse;
import com.voum.modules.marketplace.dto.RideOfferUpdateRequest;
import com.voum.modules.marketplace.service.RideOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class RideOfferController {

    private final RideOfferService rideOfferService;

    @PostMapping("/api/v1/offers")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<RideOfferResponse>> submitOffer(
            @AuthenticationPrincipal UUID motariId,
            @Valid @RequestBody RideOfferCreateRequest req) {
        RideOfferResponse response = rideOfferService.submitOffer(motariId, req);
        return ResponseEntity.ok(ApiResponse.success(response, "Ride offer submitted successfully."));
    }

    @PutMapping("/api/v1/offers/{id}")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<RideOfferResponse>> updateOffer(
            @AuthenticationPrincipal UUID motariId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody RideOfferUpdateRequest req) {
        RideOfferResponse response = rideOfferService.updateOffer(motariId, id, req);
        return ResponseEntity.ok(ApiResponse.success(response, "Ride offer updated successfully."));
    }

    @DeleteMapping("/api/v1/offers/{id}")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<Void>> withdrawOffer(
            @AuthenticationPrincipal UUID motariId,
            @PathVariable("id") UUID id) {
        rideOfferService.withdrawOffer(motariId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Ride offer withdrawn successfully."));
    }

    @GetMapping("/api/v1/requests/{id}/offers")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<List<RideOfferResponse>>> getOffers(
            @AuthenticationPrincipal UUID passengerId,
            @PathVariable("id") UUID requestId) {
        List<RideOfferResponse> offers = rideOfferService.getOffersForRequest(passengerId, requestId);
        return ResponseEntity.ok(ApiResponse.success(offers, "Offers for ride request retrieved successfully."));
    }

    @PostMapping("/api/v1/offers/{id}/accept")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<Void>> acceptOffer(
            @AuthenticationPrincipal UUID passengerId,
            @PathVariable("id") UUID id) {
        rideOfferService.acceptOffer(passengerId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Ride offer accepted successfully."));
    }
}
