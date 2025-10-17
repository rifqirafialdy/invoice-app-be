package com.invoiceapp.auth.infrastructure.security;

import com.invoiceapp.common.constants.AppConstants;
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

    private static final String ISSUER = "invoice-app";
    private static final String ALGORITHM = "HS256";

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
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(AppConstants.ACCESS_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .subject(email)
                .claim("userId", userId.toString())
                .claim("email", email)
                .build();

        return encodeToken(jwtEncoder, claims);
    }

    public String generateRefreshToken(UUID userId, String email) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(AppConstants.REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS))
                .subject(email)
                .claim("userId", userId.toString())
                .build();

        return encodeToken(refreshTokenEncoder, claims);
    }

    public String generateVerificationToken(String email) {
        return generateTokenWithType(email, "email_verification",
                AppConstants.EMAIL_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);
    }

    public String generatePasswordResetToken(String email) {
        return generateTokenWithType(email, "password_reset",
                AppConstants.PASSWORD_RESET_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);
    }

    public String generateEmailChangeToken(String oldEmail, String newEmail, UUID userId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(AppConstants.EMAIL_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .subject(oldEmail)
                .claim("userId", userId.toString())
                .claim("newEmail", newEmail)
                .claim("type", "email_change")
                .build();

        return encodeToken(jwtEncoder, claims);
    }

    public String generatePublicActionToken(UUID invoiceId, String action) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(AppConstants.PUBLIC_ACTION_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS))
                .claim("invoiceId", invoiceId.toString())
                .claim("action", action)
                .build();

        return encodeToken(jwtEncoder, claims);
    }

    public Jwt validateAccessToken(String token) {
        return jwtDecoder.decode(token);
    }

    public Jwt validateRefreshToken(String token) {
        return refreshTokenDecoder.decode(token);
    }

    public Jwt validateVerificationToken(String token) {
        return jwtDecoder.decode(token);
    }

    public Jwt validatePublicActionToken(String token) {
        return jwtDecoder.decode(token);
    }

    public String extractEmail(Jwt jwt) {
        return jwt.getSubject();
    }

    public UUID extractUserId(Jwt jwt) {
        String userIdStr = jwt.getClaim("userId");
        return UUID.fromString(userIdStr);
    }

    // Helper methods
    private String generateTokenWithType(String email, String type, long amount, ChronoUnit unit) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(amount, unit))
                .subject(email)
                .claim("type", type)
                .build();

        return encodeToken(jwtEncoder, claims);
    }

    private String encodeToken(JwtEncoder encoder, JwtClaimsSet claims) {
        JwsHeader jwsHeader = JwsHeader.with(() -> ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}