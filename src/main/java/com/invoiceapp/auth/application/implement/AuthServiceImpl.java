package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.AuthService;
import com.invoiceapp.auth.application.service.EmailService;
import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.auth.infrastructure.security.JwtService;
import com.invoiceapp.auth.presentation.dto.request.LoginRequest;
import com.invoiceapp.auth.presentation.dto.request.RegisterRequest;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final EmailService emailService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .companyName(request.getCompanyName())
                .phone(request.getPhone())
                .isVerified(false)
                .build();

        user = userRepository.save(user);
        String verificationToken = tokenService.generateEmailVerificationToken(user.getId());
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);


        return AuthResponse.builder()
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        tokenService.storeRefreshToken(
                refreshToken,
                user.getId(),
                request.getDeviceInfo(),
                30
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenService.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        var jwt = jwtService.validateRefreshToken(refreshToken);
        String email = jwtService.extractEmail(jwt);
        var userId = jwtService.extractUserId(jwt);

        String newAccessToken = jwtService.generateAccessToken(userId, email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Override
    public void verifyEmail(String token) {
        UUID userId = tokenService.verifyEmailToken(token);

        if (userId == null) {
            throw new RuntimeException("Invalid or expired verification token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("Email already verified");
        }

        String verificationToken = tokenService.generateEmailVerificationToken(user.getId());
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

    }
}