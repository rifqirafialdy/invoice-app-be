package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.infrastructure.security.JwtService;
import com.invoiceapp.common.constants.AppConstants;
import com.invoiceapp.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtService jwtService;

    @Override
    public String generateEmailVerificationToken(String email) {
        deleteExistingTokensForEmail(AppConstants.REDIS_EMAIL_VERIFY_PREFIX, email);

        String token = jwtService.generateVerificationToken(email);
        String key = AppConstants.REDIS_EMAIL_VERIFY_PREFIX + token;

        redisTemplate.opsForValue().set(key, email, AppConstants.EMAIL_TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        return token;
    }

    @Override
    public String verifyEmailToken(String token) {
        String key = AppConstants.REDIS_EMAIL_VERIFY_PREFIX + token;
        String email = (String) redisTemplate.opsForValue().get(key);

        if (email == null) {
            return null;
        }

        try {
            Jwt jwt = jwtService.validateVerificationToken(token);
            String emailFromToken = jwt.getSubject();
            redisTemplate.delete(key);
            return emailFromToken;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void storeRefreshToken(String token, UUID userId, String deviceInfo, long expirationDays) {
        String key = AppConstants.REDIS_REFRESH_TOKEN_PREFIX + token;
        RefreshTokenData data = new RefreshTokenData(userId, deviceInfo, System.currentTimeMillis());
        redisTemplate.opsForValue().set(key, data, expirationDays, TimeUnit.DAYS);
    }

    @Override
    public boolean isRefreshTokenValid(String token) {
        String key = AppConstants.REDIS_REFRESH_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void revokeRefreshToken(String token) {
        String key = AppConstants.REDIS_REFRESH_TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        String pattern = AppConstants.REDIS_REFRESH_TOKEN_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                RefreshTokenData data = (RefreshTokenData) redisTemplate.opsForValue().get(key);
                if (data != null && data.userId().equals(userId)) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    @Override
    public String generatePublicActionToken(UUID invoiceId, String action) {
        String token = jwtService.generatePublicActionToken(invoiceId, action);
        String key = buildPublicActionKey(action, token);

        redisTemplate.opsForValue().set(key, invoiceId.toString(),
                AppConstants.PUBLIC_ACTION_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);

        return token;
    }

    @Override
    public UUID verifyPublicActionToken(String token, String action) {
        String key = buildPublicActionKey(action, token);
        Object obj = redisTemplate.opsForValue().get(key);

        if (obj == null) {
            throw new UnauthorizedException("Invalid or expired action token. Token may have been used.");
        }

        if (!(obj instanceof String)) {
            throw new UnauthorizedException("Invalid token format in storage.");
        }

        String invoiceIdString = (String) obj;

        try {
            jwtService.validatePublicActionToken(token);
            redisTemplate.delete(key);
            return UUID.fromString(invoiceIdString);
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw new UnauthorizedException("Invalid action token.");
        }
    }

    @Override
    public String generatePasswordResetToken(String email) {
        deleteExistingTokensForEmail(AppConstants.REDIS_PASSWORD_RESET_PREFIX, email);

        String token = jwtService.generatePasswordResetToken(email);
        String key = AppConstants.REDIS_PASSWORD_RESET_PREFIX + token;

        redisTemplate.opsForValue().set(key, email,
                AppConstants.PASSWORD_RESET_TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        return token;
    }

    @Override
    public String verifyPasswordResetToken(String token) {
        String key = AppConstants.REDIS_PASSWORD_RESET_PREFIX + token;
        String email = (String) redisTemplate.opsForValue().get(key);

        if (email == null) {
            return null;
        }

        try {
            Jwt jwt = jwtService.validateVerificationToken(token);
            String emailFromToken = jwt.getSubject();
            redisTemplate.delete(key);
            return emailFromToken;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String generateEmailChangeToken(String oldEmail, String newEmail, UUID userId) {
        deleteExistingTokensForUser(AppConstants.REDIS_EMAIL_CHANGE_PREFIX, userId);

        String token = jwtService.generateEmailChangeToken(oldEmail, newEmail, userId);
        String key = AppConstants.REDIS_EMAIL_CHANGE_PREFIX + token;
        String value = buildEmailChangeValue(oldEmail, newEmail, userId);

        redisTemplate.opsForValue().set(key, value,
                AppConstants.EMAIL_TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        return token;
    }

    @Override
    public EmailChangeData verifyEmailChangeToken(String token) {
        String key = AppConstants.REDIS_EMAIL_CHANGE_PREFIX + token;
        String value = (String) redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            jwtService.validateVerificationToken(token);
            EmailChangeData data = parseEmailChangeValue(value);
            redisTemplate.delete(key);
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    // Helper methods
    private void deleteExistingTokensForEmail(String prefix, String email) {
        String pattern = prefix + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                String storedEmail = (String) redisTemplate.opsForValue().get(key);
                if (email.equals(storedEmail)) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    private void deleteExistingTokensForUser(String prefix, UUID userId) {
        String pattern = prefix + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                String storedValue = (String) redisTemplate.opsForValue().get(key);
                if (storedValue != null && storedValue.contains(userId.toString())) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    private String buildPublicActionKey(String action, String token) {
        return AppConstants.REDIS_PUBLIC_ACTION_PREFIX + action + ":" + token;
    }

    private String buildEmailChangeValue(String oldEmail, String newEmail, UUID userId) {
        return oldEmail + "|" + newEmail + "|" + userId.toString();
    }

    private EmailChangeData parseEmailChangeValue(String value) {
        String[] parts = value.split("\\|");
        if (parts.length == 3) {
            return new EmailChangeData(parts[0], parts[1], UUID.fromString(parts[2]));
        }
        return null;
    }

    private record RefreshTokenData(UUID userId, String deviceInfo, Long createdAt) {}
}