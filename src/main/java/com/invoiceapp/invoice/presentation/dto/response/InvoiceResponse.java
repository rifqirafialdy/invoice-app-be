package com.invoiceapp.invoice.presentation.dto.response;

import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.domain.enums.RecurringFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private UUID id;
    private UUID clientId;
    private String clientName;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String displayStatus;
    private List<InvoiceItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String notes;
    private Boolean isRecurring;
    private RecurringFrequency recurringFrequency;
    private LocalDate nextGenerationDate;
    private UUID recurringSeriesId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}