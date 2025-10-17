package com.invoiceapp.invoice.application.mapper;

import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.entity.InvoiceItem;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceItemResponse;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(invoice.getClient() != null ? invoice.getClient().getName() : "Unknown Client")
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .displayStatus(invoice.getStatus().name())
                .items(itemResponses)
                .subtotal(invoice.getSubtotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .total(invoice.getTotal())
                .notes(invoice.getNotes())
                .isRecurring(invoice.getIsRecurring())
                .recurringFrequency(invoice.getRecurringFrequency())
                .nextGenerationDate(invoice.getNextGenerationDate())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .total(item.getTotal())
                .build();
    }
}