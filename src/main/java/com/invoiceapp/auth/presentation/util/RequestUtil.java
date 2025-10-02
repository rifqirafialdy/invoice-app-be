package com.invoiceapp.auth.presentation.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RequestUtil {

    public String extractAccessTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}