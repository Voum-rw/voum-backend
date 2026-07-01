package com.voum.modules.trip.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.trip.dto.TripCancelRequest;
import com.voum.modules.trip.dto.TripResponse;
import com.voum.modules.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI')")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId) {
        TripResponse response = tripService.getTrip(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip retrieved successfully."));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @AuthenticationPrincipal UUID userId) {
        List<TripResponse> responses = tripService.getMyTrips(userId);
        return ResponseEntity.ok(ApiResponse.success(responses, "Trips retrieved successfully."));
    }

    @PostMapping("/{id}/en-route")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<TripResponse>> markEnRoute(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId) {
        TripResponse response = tripService.markEnRoute(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Motari is now en route to pickup location."));
    }

    @PostMapping("/{id}/arrived")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<TripResponse>> markArrived(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId) {
        TripResponse response = tripService.markArrived(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Motari has arrived at pickup location."));
    }

    @PostMapping("/{id}/boarded")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ApiResponse<TripResponse>> markBoarded(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId) {
        TripResponse response = tripService.markBoarded(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passenger onboarding confirmed."));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<TripResponse>> startTrip(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId) {
        TripResponse response = tripService.startTrip(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip started."));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<TripResponse>> completeTrip(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId) {
        TripResponse response = tripService.completeTrip(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip completed."));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER', 'MOTARI')")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTrip(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TripCancelRequest req) {
        TripResponse response = tripService.cancelTrip(id, userId, req.getCancellationReason());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip cancelled successfully."));
    }
}
