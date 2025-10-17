package com.invoiceapp.client.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // Add this annotation

public class ClientResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String paymentPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}