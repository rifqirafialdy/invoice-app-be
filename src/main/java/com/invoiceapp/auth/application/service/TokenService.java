package com.invoiceapp.auth.application.service;

import java.util.Map;
import java.util.UUID;

public interface TokenService {
    String generateEmailVerificationToken(String email);
    String verifyEmailToken(String token);
    void storeRefreshToken(String token, UUID userId, String deviceInfo, long expirationDays);
    boolean isRefreshTokenValid(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserTokens(UUID userId);
    String generatePublicActionToken(UUID invoiceId, String action);
    UUID verifyPublicActionToken(String token, String action);
    String generatePasswordResetToken(String email);
    String verifyPasswordResetToken(String token);

    String generateEmailChangeToken(String oldEmail, String newEmail, UUID userId);
    EmailChangeData verifyEmailChangeToken(String token);

    record EmailChangeData(String oldEmail, String newEmail, UUID userId) {}




}