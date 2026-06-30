package com.voum.modules.location.controller;

import com.voum.common.ApiResponse;
import com.voum.modules.location.dto.LocationUpdateRequest;
import com.voum.modules.location.dto.NearbyMotariResponse;
import com.voum.modules.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/go-online")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<Void>> goOnline(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody LocationUpdateRequest req) {
        locationService.goOnline(userId, req);
        return ResponseEntity.ok(ApiResponse.success(null, "Driver status transitioned to ONLINE."));
    }

    @PostMapping("/go-offline")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<Void>> goOffline(
            @AuthenticationPrincipal UUID userId) {
        locationService.goOffline(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Driver status transitioned to OFFLINE."));
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody LocationUpdateRequest req) {
        locationService.updateLocation(userId, req);
        return ResponseEntity.ok(ApiResponse.success(null, "Coordinates updated successfully."));
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('MOTARI')")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @AuthenticationPrincipal UUID userId) {
        locationService.heartbeat(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Heartbeat registered."));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyMotariResponse>>> getNearby(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("radiusKm") Double radiusKm) {
        List<NearbyMotariResponse> nearby = locationService.findNearbyMotaris(latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(nearby, "Nearby active Motaris retrieved successfully."));
    }
}
