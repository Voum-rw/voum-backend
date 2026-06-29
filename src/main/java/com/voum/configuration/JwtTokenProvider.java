package com.voum.configuration;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.security.jwt.secret:dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3Ryb25nLWtleS1mb3Itdm91bS1wbGF0Zm9ybS1iYWNrZW5kLXNlcnZpY2Vz}")
    private String jwtSecret;

    @Value("${app.security.jwt.access-expiration-minutes:15}")
    private long accessExpirationMinutes;

    @Value("${app.security.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        // Ensure secret is long enough for HMAC-SHA256
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(UUID userId, String phone, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("phone", phone)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token signature/expiration: {}", e.getMessage());
        }
        return false;
    }

    public Instant getRefreshTokenExpiry() {
        return Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS);
    }
}
