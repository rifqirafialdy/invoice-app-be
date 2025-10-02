package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.infrastructure.security.JwtService;
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
}