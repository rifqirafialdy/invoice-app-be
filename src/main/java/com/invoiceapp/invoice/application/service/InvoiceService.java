package com.invoiceapp.invoice.application.service;

import com.invoiceapp.invoice.presentation.dto.request.InvoiceRequest;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request, UUID userId);
    InvoiceResponse updateInvoice(UUID invoiceId, InvoiceRequest request, UUID userId);
    void deleteInvoice(UUID invoiceId, UUID userId);
    InvoiceResponse getInvoiceById(UUID invoiceId, UUID userId);
    Page<InvoiceResponse> getAllInvoices(UUID userId, Pageable pageable);
}