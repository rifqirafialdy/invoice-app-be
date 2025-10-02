package com.invoiceapp.auth.presentation.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final int ACCESS_TOKEN_MAX_AGE = 15 * 60;
    private static final int REFRESH_TOKEN_MAX_AGE = 30 * 24 * 60 * 60;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}