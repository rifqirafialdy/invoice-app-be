package com.invoiceapp.invoice.application.service;

import com.invoiceapp.common.dto.PageDTO;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.presentation.dto.request.InvoiceRequest;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request, UUID userId);

    InvoiceResponse updateInvoice(UUID invoiceId, InvoiceRequest request, UUID userId);

    void deleteInvoice(UUID invoiceId, UUID userId);

    InvoiceResponse getInvoiceById(UUID invoiceId, UUID userId);

    PageDTO<InvoiceResponse> getAllInvoices(UUID userId, int page, int size, String sortBy, String sortDir,
                                            String search, InvoiceStatus status, LocalDate startDate, LocalDate endDate, Boolean isRecurring);

    InvoiceResponse stopRecurring(UUID invoiceId, UUID userId);

    InvoiceResponse approveCancellation(UUID invoiceId, UUID userId);
    InvoiceResponse rejectCancellation(UUID invoiceId, UUID userId);
    InvoiceResponse confirmPayment(UUID invoiceId, UUID userId);
    InvoiceResponse rejectPayment(UUID invoiceId, UUID userId);

}
