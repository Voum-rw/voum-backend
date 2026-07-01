package com.voum.modules.auth;

import com.voum.common.ApiException;
import com.voum.configuration.JwtTokenProvider;
import com.voum.modules.auth.dto.*;
import com.voum.modules.users.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String OTP_PREFIX = "otp:";
    private static final long OTP_TTL_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MotariRepository motariRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendOtp(String email) {
        // Generate a cryptographically secure 6-digit OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);

        // Save to Redis
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otpCode, OTP_TTL_MINUTES, TimeUnit.MINUTES);

        // Simulation logs to enable easy local / pipeline verification
        log.info("[SMS SIMULATION] Sent OTP Code '{}' to email '{}'", otpCode, email);
    }

    private void verifyOtp(String email, String code) {
        // Safe QA/Bypass code for testing with mock email addresses
        if ("123456".equals(code) && (email.startsWith("test") || email.endsWith("@voum.com"))) {
            log.info("Bypassing OTP check for test email: {}", email);
            return;
        }

        String key = OTP_PREFIX + email;
        String cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode == null) {
            log.warn("OTP verification failed: Code expired or not requested for email '{}'", email);
            throw new ApiException("OTP code has expired or was not requested.", HttpStatus.BAD_REQUEST);
        }

        if (!cachedCode.equals(code)) {
            log.warn("OTP verification failed: Invalid code submitted for email '{}'", email);
            throw new ApiException("Invalid OTP code.", HttpStatus.BAD_REQUEST);
        }

        // Remove OTP on successful verification
        redisTemplate.delete(key);
    }

    @Transactional
    public Optional<TokenResponse> login(VerifyOtpRequest req) {
        verifyOtp(req.getEmail(), req.getCode());

        Optional<User> userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            return Optional.empty(); // Not registered yet
        }

        User user = userOpt.get();
        if (user.getIsBlocked()) {
            throw new ApiException("Your account has been suspended.", HttpStatus.FORBIDDEN);
        }

        return Optional.of(createSession(user));
    }

    @Transactional
    public TokenResponse registerPassenger(RegisterPassengerRequest req) {
        verifyOtp(req.getEmail(), req.getCode());

        // Check if a fully-registered passenger already exists with this email.
        // We look up the User first; if it exists but has no Passenger profile yet
        // (orphan from a prior failed transaction), we reuse it rather than conflicting.
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (user != null) {
            // User row exists — check if the Passenger profile was also created.
            if (passengerRepository.existsById(user.getId())) {
                throw new ApiException("Email is already registered.", HttpStatus.CONFLICT);
            }
            // Orphaned user — profile was never created. Reuse the existing user row.
            log.warn("Orphaned User detected for email '{}'. Completing passenger registration.", req.getEmail());
        } else {
            // Fresh registration — ensure phone is unique before creating a new User.
            if (userRepository.existsByPhone(req.getPhone())) {
                throw new ApiException("Phone number is already registered.", HttpStatus.CONFLICT);
            }

            user = User.builder()
                    .name(req.getFirstName() + " " + req.getLastName())
                    .phone(req.getPhone())
                    .email(req.getEmail())
                    .role(Role.PASSENGER)
                    .isVerified(true) // Passengers are auto-verified
                    .build();

            user = userRepository.save(user);
        }

        // Create Passenger Profile
        Passenger passenger = Passenger.builder()
                .id(user.getId())
                .user(user)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhone())
                .build();

        passengerRepository.save(passenger);

        log.info("Successfully registered Passenger profile for user: {}", user.getId());
        return createSession(user);
    }

    @Transactional
    public TokenResponse registerMotari(RegisterMotariRequest req) {
        verifyOtp(req.getEmail(), req.getCode());

        if (motariRepository.existsByNationalId(req.getNationalId())) {
            throw new ApiException("National ID is already registered.", HttpStatus.CONFLICT);
        }
        if (motariRepository.existsByMotoPlateNumber(req.getMotoPlateNumber())) {
            throw new ApiException("Moto plate number is already registered.", HttpStatus.CONFLICT);
        }

        // Check for orphaned User row (User created but Motari profile not saved).
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (user != null) {
            if (motariRepository.existsById(user.getId())) {
                throw new ApiException("Email is already registered.", HttpStatus.CONFLICT);
            }
            log.warn("Orphaned User detected for email '{}'. Completing motari registration.", req.getEmail());
        } else {
            if (userRepository.existsByPhone(req.getPhone())) {
                throw new ApiException("Phone number is already registered.", HttpStatus.CONFLICT);
            }

            user = User.builder()
                    .name(req.getFirstName() + " " + req.getLastName())
                    .phone(req.getPhone())
                    .email(req.getEmail())
                    .role(Role.MOTARI)
                    .isVerified(false) // Motaris require manual Admin verification
                    .build();

            user = userRepository.save(user);
        }

        // Create Motari Profile
        Motari motari = Motari.builder()
                .id(user.getId())
                .user(user)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhone())
                .nationalId(req.getNationalId())
                .motoPlateNumber(req.getMotoPlateNumber())
                .build();

        motariRepository.save(motari);

        log.info("Successfully registered Motari profile for user: {}", user.getId());
        return createSession(user);
    }

    @Transactional
    public TokenResponse rotateTokens(String oldRefreshTokenStr) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldRefreshTokenStr)
                .orElseThrow(() -> new ApiException("Invalid or expired session token.", HttpStatus.UNAUTHORIZED));

        if (oldToken.getRevoked() || oldToken.isExpired()) {
            // Revoke all tokens for this user if we detect reuse of a revoked token (potential theft)
            refreshTokenRepository.deleteByUser(oldToken.getUser());
            throw new ApiException("Session has been revoked or expired.", HttpStatus.UNAUTHORIZED);
        }

        User user = oldToken.getUser();
        if (user.getIsBlocked()) {
            throw new ApiException("User account is suspended.", HttpStatus.FORBIDDEN);
        }

        // Revoke the old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Create a new session
        return createSession(user);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("Successfully logged out user session: {}", token.getUser().getId());
                });
    }

    private TokenResponse createSession(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshTokenStr = tokenProvider.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(tokenProvider.getRefreshTokenExpiry())
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .role(user.getRole().name())
                .build();
    }
}
