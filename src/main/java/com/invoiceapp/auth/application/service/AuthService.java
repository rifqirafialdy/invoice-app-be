package com.invoiceapp.auth.application.service;

import com.invoiceapp.auth.presentation.dto.request.ForgotPasswordRequest;
import com.invoiceapp.auth.presentation.dto.request.LoginRequest;
import com.invoiceapp.auth.presentation.dto.request.RegisterRequest;
import com.invoiceapp.auth.presentation.dto.request.ResetPasswordRequest;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;


public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);



}