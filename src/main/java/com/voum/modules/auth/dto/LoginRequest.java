package com.voum.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email or phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;
}
