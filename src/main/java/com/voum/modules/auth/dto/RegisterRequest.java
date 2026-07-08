package com.voum.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?250|0)?7[2389]\\d{7}$", message = "Invalid Rwandan phone number")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "PASSENGER|MOTARI", message = "Role must be PASSENGER or MOTARI")
    private String role;

    // Motari-only extras (required when role = MOTARI)
    private String nationalId;
    private String motoPlateNumber;
}
