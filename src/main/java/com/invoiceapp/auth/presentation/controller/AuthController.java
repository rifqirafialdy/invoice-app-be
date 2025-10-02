package com.invoiceapp.auth.presentation.controller;

import com.invoiceapp.auth.application.service.AuthService;
import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.auth.infrastructure.security.JwtService;
import com.invoiceapp.auth.presentation.dto.request.*;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;
import com.invoiceapp.auth.presentation.util.CookieUtil;
import com.invoiceapp.auth.presentation.util.RequestUtil;
import com.invoiceapp.common.dto.ApiResponse;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final RequestUtil requestUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessTokenCookie(authResponse.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshTokenCookie(authResponse.getRefreshToken()).toString())
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessTokenCookie(authResponse.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshTokenCookie(authResponse.getRefreshToken()).toString())
                .body(ApiResponse.success("Login successful", authResponse)); // Return authResponse, not a string
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(HttpServletRequest request) {
        String refreshToken = requestUtil.extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token not found"));
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessTokenCookie(authResponse.getAccessToken()).toString())
                .body(ApiResponse.success("Token refreshed", "Token refreshed successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String refreshToken = requestUtil.extractRefreshTokenFromCookies(request);

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearCookie("accessToken").toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearCookie("refreshToken").toString())
                .body(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(HttpServletRequest request) {
        String accessToken = requestUtil.extractAccessTokenFromCookies(request);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        try {
            var jwt = jwtService.validateAccessToken(accessToken);
            String email = jwtService.extractEmail(jwt);

            authService.resendVerificationEmail(email);

            return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(HttpServletRequest request) {
        String accessToken = requestUtil.extractAccessTokenFromCookies(request);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var jwt = jwtService.validateAccessToken(accessToken);
            String email = jwtService.extractEmail(jwt);
            UUID userId = jwtService.extractUserId(jwt);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            return ResponseEntity.ok(AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .companyName(user.getCompanyName())
                    .isVerified(user.getIsVerified())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}