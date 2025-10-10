package com.invoiceapp.dashboard.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String id;
    private String type;
    private String message;
    private LocalDateTime timestamp;
    private String invoiceNumber;
    private String clientName;
}