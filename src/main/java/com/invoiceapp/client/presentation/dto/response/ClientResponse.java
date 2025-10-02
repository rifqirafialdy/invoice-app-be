package com.invoiceapp.client.presentation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
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