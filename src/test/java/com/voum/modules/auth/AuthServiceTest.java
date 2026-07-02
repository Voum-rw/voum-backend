package com.voum.modules.auth;

import com.voum.common.ApiException;
import com.voum.configuration.JwtTokenProvider;
import com.voum.modules.auth.dto.*;
import com.voum.modules.notification.EmailService;
import com.voum.modules.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

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
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Setup Redis Operations mock
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendOtp_shouldStoreInRedis() {
        String email = "test@voum.com";

        authService.sendOtp(email);

        verify(valueOperations, times(1)).set(
                eq("otp:" + email),
                anyString(),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        // EmailService should be called once with the email and generated OTP
        verify(emailService, times(1)).sendOtpEmail(eq(email), anyString());
    }

    @Test
    void registerPassenger_shouldCreateUserAndProfile() {
        String phone = "+250780000000";
        String email = "john@voum.rw";
        String code = "123456";
        
        RegisterPassengerRequest req = new RegisterPassengerRequest();
        req.setPhone(phone);
        req.setEmail(email);
        req.setCode(code);
        req.setFirstName("John");
        req.setLastName("Doe");

        // Mock OTP check
        when(valueOperations.get("otp:" + email)).thenReturn(code);
        // New flow: findByEmail is called first; returning empty means fresh registration
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.existsByPhone(phone)).thenReturn(false);

        // Mock User Save
        UUID generatedId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(generatedId)
                .name("John Doe")
                .phone(phone)
                .email(email)
                .role(Role.PASSENGER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Mock JWT generation
        when(tokenProvider.generateAccessToken(eq(generatedId), eq(phone), eq("PASSENGER"))).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh_token");
        when(tokenProvider.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));

        TokenResponse response = authService.registerPassenger(req);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("PASSENGER", response.getRole());

        verify(userRepository, times(1)).save(any(User.class));
        verify(passengerRepository, times(1)).save(any(Passenger.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void registerPassenger_withDuplicatePhone_shouldThrowApiException() {
        String phone = "+250780000000";
        String email = "john@voum.rw";
        String code = "123456";
        
        RegisterPassengerRequest req = new RegisterPassengerRequest();
        req.setPhone(phone);
        req.setEmail(email);
        req.setCode(code);

        when(valueOperations.get("otp:" + email)).thenReturn(code);
        when(userRepository.existsByPhone(phone)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.registerPassenger(req));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
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
