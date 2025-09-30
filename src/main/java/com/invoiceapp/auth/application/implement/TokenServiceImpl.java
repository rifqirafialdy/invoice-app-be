package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String generateEmailVerificationToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        String key = "email_verify:" + token;

        redisTemplate.opsForValue().set(key, userId.toString(), 24, TimeUnit.HOURS);

        return token;
    }

    @Override
    public UUID verifyEmailToken(String token) {
        String key = "email_verify:" + token;
        String userIdStr = (String) redisTemplate.opsForValue().get(key);

        if (userIdStr != null) {
            redisTemplate.delete(key);
            return UUID.fromString(userIdStr);
        }

        return null;
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
}