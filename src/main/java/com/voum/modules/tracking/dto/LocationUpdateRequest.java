package com.voum.modules.tracking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequest {

    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Accuracy is required")
    @Min(value = 0, message = "Accuracy must be greater than or equal to 0")
    private Double accuracy;

    @NotNull(message = "Speed is required")
    @Min(value = 0, message = "Speed must be greater than or equal to 0")
    private Double speedKmh;

    @NotNull(message = "Heading is required")
    @Min(value = 0, message = "Heading must be between 0 and 360")
    @Max(value = 360, message = "Heading must be between 0 and 360")
    private Double headingDegrees;

    @Min(value = 0, message = "Battery level must be between 0 and 100")
    @Max(value = 100, message = "Battery level must be between 0 and 100")
    private Integer batteryLevel;

    private Boolean gpsMocked;
}
