package com.invoiceapp.invoice.presentation.dto.response;

import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {
    private UUID id;
    private UUID clientId;
    private String clientName;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String displayStatus; // Calculated status
    private List<InvoiceItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}