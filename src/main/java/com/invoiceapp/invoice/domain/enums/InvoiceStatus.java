package com.invoiceapp.invoice.domain.enums;

public enum InvoiceStatus {
    DRAFT,      // Not sent yet
    SENT,       // Sent, awaiting payment
    PAID,       // Payment received
    CANCELLED   // Cancelled
}
