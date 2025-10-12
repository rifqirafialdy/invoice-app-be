package com.invoiceapp.auth.presentation.controller;

import com.invoiceapp.auth.application.service.UserService;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.presentation.dto.request.ChangeEmailRequest;
import com.invoiceapp.auth.presentation.dto.request.ChangePasswordRequest;
import com.invoiceapp.common.service.FileUploadService;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import com.invoiceapp.auth.presentation.dto.request.UserProfileRequest;
import com.invoiceapp.auth.presentation.dto.response.AuthResponse;
import com.invoiceapp.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;



    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(
            @RequestAttribute("userId") UUID userId
    ) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            return ResponseEntity.ok(AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .companyName(user.getCompanyName())
                    .isVerified(user.getIsVerified())
                    .logoUrl(user.getLogoUrl())
                    .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @Valid @RequestBody UserProfileRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        AuthResponse response = userService.updateUserProfile(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/logo")
    public ResponseEntity<ApiResponse<String>> uploadUserLogo(
            @RequestAttribute("userId") UUID userId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        String[] allowedImageTypes = {
                "image/jpeg",
                "image/png",
                "image/webp",
                "image/jpg"
        };
        long maxFileSizeKB = 1024;

        String logoUrl = fileUploadService.uploadFile(file, allowedImageTypes, maxFileSizeKB);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLogoUrl(logoUrl);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Logo uploaded successfully", logoUrl));
    }
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestAttribute("userId") UUID userId) {
        userService.changePassword(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
    @PostMapping("/change-email")
    public ResponseEntity<ApiResponse<String>> changeEmail(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody ChangeEmailRequest request) {
        userService.changeEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent to new address"));
    }

    @GetMapping("/verify-email-change")
    public ResponseEntity<ApiResponse<String>> verifyEmailChange(@RequestParam String token) {
        userService.verifyEmailChange(token);
        return ResponseEntity.ok(ApiResponse.success("Email changed successfully"));
    }




}