package com.voum.modules.auth;

import com.voum.common.ApiException;
import com.voum.configuration.JwtTokenProvider;
import com.voum.modules.auth.dto.*;
import com.voum.modules.users.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MotariRepository motariRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    // ──────────────────────────────────────────────────────────────────────────
    // REGISTER  (phone + password, no OTP)
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String phone = req.getPhone().trim();
        String role  = req.getRole().trim().toUpperCase();

        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("This phone number is already registered.", HttpStatus.CONFLICT);
        }

        String hashedPassword = passwordEncoder.encode(req.getPassword());

        User user = User.builder()
                .name(req.getFullName().trim())
                .phone(phone)
                .password(hashedPassword)
                .role(Role.valueOf(role))
                .isVerified(role.equals("PASSENGER"))   // Passengers auto-verified; Motaris need admin
                .build();

        user = userRepository.save(user);

        if (role.equals("PASSENGER")) {
            Passenger passenger = Passenger.builder()
                    .id(user.getId())
                    .user(user)
                    .firstName(extractFirstName(req.getFullName()))
                    .lastName(extractLastName(req.getFullName()))
                    .phoneNumber(phone)
                    .build();
            passengerRepository.save(passenger);
            log.info("Passenger registered: {}", user.getId());

        } else if (role.equals("MOTARI")) {
            validateMotariExtras(req);

            Motari motari = Motari.builder()
                    .id(user.getId())
                    .user(user)
                    .firstName(extractFirstName(req.getFullName()))
                    .lastName(extractLastName(req.getFullName()))
                    .phoneNumber(phone)
                    .nationalId(req.getNationalId().trim())
                    .motoPlateNumber(req.getMotoPlateNumber().trim().toUpperCase())
                    .build();
            motariRepository.save(motari);
            log.info("Motari registered (pending admin approval): {}", user.getId());
        }

        return createSession(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // LOGIN  (phone + password)
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String phone = req.getPhone().trim();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApiException(
                        "Account not found. Please sign up first.", HttpStatus.NOT_FOUND));

        if (user.getIsBlocked()) {
            throw new ApiException("Your account has been suspended.", HttpStatus.FORBIDDEN);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ApiException("Incorrect password.", HttpStatus.UNAUTHORIZED);
        }

        log.info("User logged in: {}", user.getId());
        return createSession(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TOKEN ROTATION  (refresh token)
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse rotateTokens(String oldRefreshTokenStr) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldRefreshTokenStr)
                .orElseThrow(() -> new ApiException("Invalid or expired session token.", HttpStatus.UNAUTHORIZED));

        if (oldToken.getRevoked() || oldToken.isExpired()) {
            // Potential token theft — revoke all sessions for this user
            refreshTokenRepository.deleteByUser(oldToken.getUser());
            throw new ApiException("Session has been revoked or expired.", HttpStatus.UNAUTHORIZED);
        }

        User user = oldToken.getUser();
        if (user.getIsBlocked()) {
            throw new ApiException("User account is suspended.", HttpStatus.FORBIDDEN);
        }

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        return createSession(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("User logged out: {}", token.getUser().getId());
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private TokenResponse createSession(User user) {
        String accessToken      = tokenProvider.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshTokenStr  = tokenProvider.generateRefreshToken();

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
                .userName(user.getName())
                .userPhone(user.getPhone())
                .userId(user.getId())
                .build();
    }

    private void validateMotariExtras(RegisterRequest req) {
        if (req.getNationalId() == null || req.getNationalId().isBlank()) {
            throw new ApiException("National ID is required for Motari registration.", HttpStatus.BAD_REQUEST);
        }
        if (req.getMotoPlateNumber() == null || req.getMotoPlateNumber().isBlank()) {
            throw new ApiException("Moto plate number is required for Motari registration.", HttpStatus.BAD_REQUEST);
        }
        if (motariRepository.existsByNationalId(req.getNationalId().trim())) {
            throw new ApiException("National ID is already registered.", HttpStatus.CONFLICT);
        }
        if (motariRepository.existsByMotoPlateNumber(req.getMotoPlateNumber().trim().toUpperCase())) {
            throw new ApiException("Moto plate number is already registered.", HttpStatus.CONFLICT);
        }
    }

    private String extractFirstName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts[0];
    }

    private String extractLastName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }
}
