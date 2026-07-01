package com.voum.modules.tracking.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.tracking.dto.LocationUpdateRequest;
import com.voum.modules.tracking.dto.TripEtaResponse;
import com.voum.modules.tracking.dto.TripLocationResponse;
import com.voum.modules.tracking.service.TripTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}")
@RequiredArgsConstructor
public class TripTrackingController {

    private final TripTrackingService tripTrackingService;

    @PutMapping("/location")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<TripLocationResponse>> updateLocation(
            @PathVariable("tripId") UUID tripId,
            @AuthenticationPrincipal UUID driverId,
            @Valid @RequestBody LocationUpdateRequest req) {
        TripLocationResponse response = tripTrackingService.updateLocation(tripId, driverId, req);
        return ResponseEntity.ok(ApiResponse.success(response, "Location updated successfully."));
    }

    @GetMapping("/current-location")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI')")
    public ResponseEntity<ApiResponse<TripLocationResponse>> getCurrentLocation(
            @PathVariable("tripId") UUID tripId,
            @AuthenticationPrincipal UUID userId) {
        TripLocationResponse response = tripTrackingService.getCurrentLocation(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Current location retrieved successfully."));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI')")
    public ResponseEntity<ApiResponse<List<TripLocationResponse>>> getLocationHistory(
            @PathVariable("tripId") UUID tripId,
            @AuthenticationPrincipal UUID userId) {
        List<TripLocationResponse> responses = tripTrackingService.getLocationHistory(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses, "Location history retrieved successfully."));
    }

    @GetMapping("/eta")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI')")
    public ResponseEntity<ApiResponse<TripEtaResponse>> getEta(
            @PathVariable("tripId") UUID tripId,
            @AuthenticationPrincipal UUID userId) {
        TripEtaResponse response = tripTrackingService.getEta(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Estimated arrival time calculated."));
    }
}
