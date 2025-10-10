package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.EmailService;
import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.application.service.UserService;
import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.auth.infrastructure.security.JwtService;
import com.invoiceapp.auth.presentation.dto.request.ChangeEmailRequest;
import com.invoiceapp.auth.presentation.dto.request.ChangePasswordRequest;
import com.invoiceapp.auth.presentation.dto.request.UserProfileRequest;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;
import com.invoiceapp.common.exception.BadRequestException;
import com.invoiceapp.common.exception.ResourceConflictException;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;


import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final JwtService jwtService;



    @Override
    public AuthResponse updateUserProfile(UserProfileRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getCompanyName() != null) {
            user.setCompanyName(request.getCompanyName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }


        userRepository.save(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .companyName(user.getCompanyName())
                .isVerified(user.getIsVerified())
                .message("User profile updated successfully")
                .build();
    }

    @Override
    public void changePassword(ChangePasswordRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    @Override
    public void changeEmail(UUID userId, ChangeEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (user.getEmail().equals(request.getNewEmail())) {
            throw new BadRequestException("New email must be different from current email");
        }

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new BadRequestException("Email already in use");
        }

        String changeToken = tokenService.generateEmailChangeToken(
                user.getEmail(),
                request.getNewEmail(),
                user.getId()
        );

        emailService.sendEmailChangeVerification(request.getNewEmail(), changeToken);

        log.info("Email change verification sent to new email: {}", request.getNewEmail());
    }

    @Override
    public void verifyEmailChange(String token) {
        TokenService.EmailChangeData data = tokenService.verifyEmailChangeToken(token);

        if (data == null) {
            try {
                Jwt jwt = jwtService.validateVerificationToken(token);
                throw new BadRequestException("Email change link has expired. Please request a new one.");
            } catch (JwtException e) {
                throw new BadRequestException("Invalid or expired email change token");
            }
        }

        User user = userRepository.findById(data.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmail(data.newEmail())) {
            throw new BadRequestException("Email already in use");
        }

        String oldEmail = user.getEmail();
        user.setEmail(data.newEmail());
        userRepository.save(user);

        emailService.sendEmailChangeNotification(oldEmail, data.newEmail());

        log.info("Email changed successfully from {} to {}", oldEmail, data.newEmail());
    }


}