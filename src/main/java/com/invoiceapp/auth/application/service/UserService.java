package com.invoiceapp.auth.application.service;

import com.invoiceapp.auth.presentation.dto.request.ChangePasswordRequest;
import com.invoiceapp.auth.presentation.dto.request.UserProfileRequest;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;
import com.invoiceapp.auth.presentation.dto.request.ChangeEmailRequest;
import java.util.UUID;

public interface UserService {
    AuthResponse updateUserProfile(UserProfileRequest request, UUID userId);
    void changePassword(ChangePasswordRequest request, UUID userId);
    void changeEmail(UUID userId, ChangeEmailRequest request);
    void verifyEmailChange(String token);



}