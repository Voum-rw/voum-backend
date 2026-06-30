package com.voum.modules.marketplace.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.marketplace.dto.RideRequestCreateRequest;
import com.voum.modules.marketplace.dto.RideRequestResponse;
import com.voum.modules.marketplace.dto.RideRequestStatusResponse;
import com.voum.modules.marketplace.service.RideRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RideRequestController {

    private final RideRequestService rideRequestService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<RideRequestResponse>> createRequest(
            @AuthenticationPrincipal UUID passengerId,
            @Valid @RequestBody RideRequestCreateRequest req) {
        RideRequestResponse response = rideRequestService.createRequest(passengerId, req);
        return ResponseEntity.ok(ApiResponse.success(response, "Ride request created successfully."));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<List<RideRequestResponse>>> getMyRequests(
            @AuthenticationPrincipal UUID passengerId) {
        List<RideRequestResponse> requests = rideRequestService.getMyRequests(passengerId);
        return ResponseEntity.ok(ApiResponse.success(requests, "Passenger ride requests retrieved."));
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<List<RideRequestResponse>>> getNearbyRequests(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("radiusKm") Double radiusKm) {
        List<RideRequestResponse> requests = rideRequestService.findNearbyRequests(latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(requests, "Nearby open ride requests retrieved."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RideRequestResponse>> getRequest(
            @PathVariable("id") UUID id) {
        RideRequestResponse request = rideRequestService.getRequest(id);
        return ResponseEntity.ok(ApiResponse.success(request, "Ride request retrieved successfully."));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RideRequestStatusResponse>> getStatus(
            @PathVariable("id") UUID id) {
        RideRequestStatusResponse status = rideRequestService.getRequestStatus(id);
        return ResponseEntity.ok(ApiResponse.success(status, "Ride request status retrieved successfully."));
    }
}
