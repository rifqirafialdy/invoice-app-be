package com.invoiceapp.invoice.presentation.dto.response;

import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicInvoiceResponse {
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String displayStatus;

    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String companyAddress;
    private String companyLogoUrl;

    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String clientAddress;

    private List<PublicInvoiceItemResponse> items;

    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal total;

    private String notes;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PublicInvoiceItemResponse {
        private String productName;
        private String productDescription;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
    }
}