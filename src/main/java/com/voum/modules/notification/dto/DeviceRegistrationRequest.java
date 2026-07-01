package com.voum.modules.notification.dto;

import com.voum.modules.notification.entity.DeviceToken.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegistrationRequest {

    @NotBlank(message = "Device token must not be blank")
    private String deviceToken;

    @NotNull(message = "Platform must be specified (ANDROID or IOS)")
    private Platform platform;

    private String appVersion;
}
