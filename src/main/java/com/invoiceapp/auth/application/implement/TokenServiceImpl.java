package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.infrastructure.security.JwtService;
import com.invoiceapp.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;


    @Override
    public String generateEmailVerificationToken(String email) {
        String pattern = "email_verify:*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String storedEmail = (String) redisTemplate.opsForValue().get(key);
                if (email.equals(storedEmail)) {
                    redisTemplate.delete(key);
                }
            }
        }

        String token = jwtService.generateVerificationToken(email);

        String key = "email_verify:" + token;
        redisTemplate.opsForValue().set(key, email, 1, TimeUnit.HOURS);

        return token;
    }

    @Override
    public String verifyEmailToken(String token) {
        String key = "email_verify:" + token;

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
        String key = "refresh_token:" + token;

        RefreshTokenData data = new RefreshTokenData(userId, deviceInfo, System.currentTimeMillis());

        redisTemplate.opsForValue().set(key, data, expirationDays, TimeUnit.DAYS);
    }

    @Override
    public boolean isRefreshTokenValid(String token) {
        String key = "refresh_token:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void revokeRefreshToken(String token) {
        String key = "refresh_token:" + token;
        redisTemplate.delete(key);
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        var keys = redisTemplate.keys("refresh_token:*");
        if (keys != null) {
            for (String key : keys) {
                RefreshTokenData data = (RefreshTokenData) redisTemplate.opsForValue().get(key);
                if (data != null && data.userId().equals(userId)) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    private record RefreshTokenData(UUID userId, String deviceInfo, Long createdAt) {
    }
    @Override
    public String generatePublicActionToken(UUID invoiceId, String action) {
        String token = jwtService.generatePublicActionToken(invoiceId, action);

        String key = "public_action:" + action + ":" + token;

        redisTemplate.opsForValue().set(key, invoiceId, 7, TimeUnit.DAYS);

        return token;
    }
    @Override
    public UUID verifyPublicActionToken(String token, String action) {
        String key = "public_action:" + action + ":" + token;


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
        String pattern = "password_reset:*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String storedEmail = (String) redisTemplate.opsForValue().get(key);
                if (email.equals(storedEmail)) {
                    redisTemplate.delete(key);
                }
            }
        }

        String token = jwtService.generatePasswordResetToken(email);
        String key = "password_reset:" + token;
        redisTemplate.opsForValue().set(key, email, 1, TimeUnit.HOURS);

        return token;
    }

    @Override
    public String verifyPasswordResetToken(String token) {
        String key = "password_reset:" + token;
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
        String pattern = "email_change:*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String storedValue = (String) redisTemplate.opsForValue().get(key);
                if (storedValue != null && storedValue.contains(userId.toString())) {
                    redisTemplate.delete(key);
                }
            }
        }

        String token = jwtService.generateEmailChangeToken(oldEmail, newEmail, userId);
        String key = "email_change:" + token;

        String value = oldEmail + "|" + newEmail + "|" + userId.toString();
        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

        return token;
    }

    @Override
    public EmailChangeData verifyEmailChangeToken(String token) {
        String key = "email_change:" + token;
        String value = (String) redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            Jwt jwt = jwtService.validateVerificationToken(token);

            String[] parts = value.split("\\|");
            if (parts.length == 3) {
                String oldEmail = parts[0];
                String newEmail = parts[1];
                UUID userId = UUID.fromString(parts[2]);

                redisTemplate.delete(key);
                return new EmailChangeData(oldEmail, newEmail, userId);
            }

            return null;
        } catch (Exception e) {
            return null;
        }

}}