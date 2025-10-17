package com.invoiceapp.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}