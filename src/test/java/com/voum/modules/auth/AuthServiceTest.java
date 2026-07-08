package com.voum.modules.auth;

import com.voum.common.ApiException;
import com.voum.configuration.JwtTokenProvider;
import com.voum.modules.auth.dto.*;
import com.voum.modules.users.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private MotariRepository motariRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndProfile() {
        String phone = "+250780000000";
        String password = "securepassword";
        
        RegisterRequest req = new RegisterRequest();
        req.setPhone(phone);
        req.setPassword(password);
        req.setFullName("John Doe");
        req.setRole("PASSENGER");

        when(userRepository.existsByPhone(phone)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashed_password");

        // Mock User Save
        UUID generatedId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(generatedId)
                .name("John Doe")
                .phone(phone)
                .role(Role.PASSENGER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Mock JWT generation
        when(tokenProvider.generateAccessToken(eq(generatedId), eq(phone), eq("PASSENGER"))).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh_token");
        when(tokenProvider.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));

        TokenResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("PASSENGER", response.getRole());

        verify(userRepository, times(1)).save(any(User.class));
        verify(passengerRepository, times(1)).save(any(Passenger.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void register_withDuplicatePhone_shouldThrowApiException() {
        String phone = "+250780000000";
        
        RegisterRequest req = new RegisterRequest();
        req.setPhone(phone);
        req.setPassword("securepassword");
        req.setFullName("John Doe");
        req.setRole("PASSENGER");

        when(userRepository.existsByPhone(phone)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void login_withCorrectCredentials_shouldReturnTokens() {
        String phone = "+250780000000";
        String password = "securepassword";
        
        LoginRequest req = new LoginRequest();
        req.setPhone(phone);
        req.setPassword(password);

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .name("John Doe")
                .password("hashed_password")
                .role(Role.PASSENGER)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(true);

        // Mock JWT generation
        when(tokenProvider.generateAccessToken(eq(userId), eq(phone), eq("PASSENGER"))).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh_token");
        when(tokenProvider.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));

        TokenResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
    }

    @Test
    void login_withIncorrectPassword_shouldThrowApiException() {
        String phone = "+250780000000";
        String password = "securepassword";
        
        LoginRequest req = new LoginRequest();
        req.setPhone(phone);
        req.setPassword(password);

        User user = User.builder()
                .phone(phone)
                .password("hashed_password")
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void rotateTokens_withRevokedToken_shouldDeleteUserSessionsAndThrowException() {
        String tokenStr = "revoked_refresh_token";
        
        User user = User.builder().id(UUID.randomUUID()).build();
        RefreshToken oldToken = RefreshToken.builder()
                .user(user)
                .token(tokenStr)
                .revoked(true) // already revoked!
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(oldToken));

        ApiException exception = assertThrows(ApiException.class, () -> authService.rotateTokens(tokenStr));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        
        // Verifies reuse detection deletes all sessions for the user
        verify(refreshTokenRepository, times(1)).deleteByUser(user);
    }
}
