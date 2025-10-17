package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.AuthService;
import com.invoiceapp.auth.application.service.EmailService;
import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.auth.infrastructure.security.JwtService;
import com.invoiceapp.auth.presentation.dto.request.*;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;
import com.invoiceapp.common.exception.BadRequestException;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import com.invoiceapp.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .companyName(request.getCompanyName())
                .phone(request.getPhone())
                .isVerified(false)
                .build();

        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());

        return login(loginRequest);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        tokenService.storeRefreshToken(refreshToken, user.getId(), request.getDeviceInfo(), 30);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenService.isRefreshTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Jwt jwt = jwtService.validateRefreshToken(refreshToken);
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
        String email = tokenService.verifyEmailToken(token);

        if (email == null) {
            handleExpiredVerificationToken(token);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsVerified()) {
            throw new BadRequestException("Your account is already verified. You can login now.");
        }

        user.setIsVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsVerified()) {
            throw new BadRequestException("Email already verified");
        }

        String verificationToken = tokenService.generateEmailVerificationToken(email);
        emailService.sendVerificationEmail(email, verificationToken);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String resetToken = tokenService.generatePasswordResetToken(user.getEmail());
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("Password reset email sent to: {}", user.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = tokenService.verifyPasswordResetToken(request.getToken());

        if (email == null) {
            try {
                jwtService.validateVerificationToken(request.getToken());
                throw new BadRequestException("Reset link has expired. Please request a new one.");
            } catch (JwtException e) {
                throw new BadRequestException("Invalid or expired reset token");
            }
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .companyName(user.getCompanyName())
                .isVerified(user.getIsVerified())
                .build();
    }

    private void handleExpiredVerificationToken(String token) {
        try {
            Jwt jwt = jwtService.validateVerificationToken(token);
            String emailFromToken = jwt.getSubject();

            User user = userRepository.findByEmail(emailFromToken)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user.getIsVerified()) {
                throw new BadRequestException("Your account is already verified. You can login now.");
            } else {
                throw new BadRequestException("This verification link has been used or a newer one was sent. Please check your email and use the latest verification link.");
            }
        } catch (JwtException e) {
            throw new BadRequestException("Verification link has expired. Please login to request a new verification link.");
        }
    }
}