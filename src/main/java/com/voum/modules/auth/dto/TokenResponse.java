package com.voum.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private String userName;   // Display name for Flutter "Welcome back, [Name]"
    private String userPhone;  // Persisted in Flutter for password-only return screen
    private java.util.UUID userId;
}
