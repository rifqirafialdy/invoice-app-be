package com.invoiceapp.auth.application.service;

import java.util.UUID;

public interface TokenService {
    String generateEmailVerificationToken(String email);
    String verifyEmailToken(String token);
    void storeRefreshToken(String token, UUID userId, String deviceInfo, long expirationDays);
    boolean isRefreshTokenValid(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserTokens(UUID userId);
}