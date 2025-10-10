package com.invoiceapp.invoice.presentation.dto.request;

import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.domain.enums.RecurringFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Status is required")
    private InvoiceStatus status;

    private Boolean isRecurring = false;
    private RecurringFrequency recurringFrequency;

    @NotNull(message = "Items are required")
    @Size(min = 1, message = "At least one item is required")
    @Valid
    private List<InvoiceItemRequest> items;

    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.0", message = "Tax rate must be positive")
    @DecimalMax(value = "100.0", message = "Tax rate cannot exceed 100%")
    private BigDecimal taxRate;

    private String notes;
}