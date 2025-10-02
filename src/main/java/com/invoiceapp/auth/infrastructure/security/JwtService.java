package com.invoiceapp.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Qualifier("jwtEncoder")
    private final JwtEncoder jwtEncoder;

    @Qualifier("jwtDecoder")
    private final JwtDecoder jwtDecoder;

    @Qualifier("refreshTokenEncoder")
    private final JwtEncoder refreshTokenEncoder;

    @Qualifier("refreshTokenDecoder")
    private final JwtDecoder refreshTokenDecoder;

    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("invoice-app")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.MINUTES))
                .subject(email)
                .claim("userId", userId.toString())
                .claim("email", email)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String generateRefreshToken(UUID userId, String email) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("invoice-app")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject(email)
                .claim("userId", userId.toString())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return refreshTokenEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public Jwt validateAccessToken(String token) {
        return jwtDecoder.decode(token);
    }

    public Jwt validateRefreshToken(String token) {
        return refreshTokenDecoder.decode(token);
    }

    public String extractEmail(Jwt jwt) {
        return jwt.getSubject();
    }

    public UUID extractUserId(Jwt jwt) {
        String userIdStr = jwt.getClaim("userId");
        return UUID.fromString(userIdStr);
    }
    public String generateVerificationToken(String email) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("invoice-app")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS)) // 1 hour expiry
                .subject(email)
                .claim("type", "email_verification")
                .build();

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public Jwt validateVerificationToken(String token) {
        return jwtDecoder.decode(token);
    }
}