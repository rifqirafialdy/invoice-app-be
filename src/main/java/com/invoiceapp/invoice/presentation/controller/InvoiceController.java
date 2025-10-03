package com.invoiceapp.invoice.presentation.controller;

import com.invoiceapp.common.dto.ApiResponse;
import com.invoiceapp.invoice.application.service.InvoiceService;
import com.invoiceapp.invoice.presentation.dto.request.InvoiceRequest;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        InvoiceResponse response = invoiceService.createInvoice(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", response));
    }

    @PutMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        InvoiceResponse response = invoiceService.updateInvoice(invoiceId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Invoice updated successfully", response));
    }

    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable UUID invoiceId,
            @RequestAttribute("userId") UUID userId
    ) {
        invoiceService.deleteInvoice(invoiceId, userId);
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted successfully", null));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable UUID invoiceId,
            @RequestAttribute("userId") UUID userId
    ) {
        InvoiceResponse response = invoiceService.getInvoiceById(invoiceId, userId);
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<InvoiceResponse> response = invoiceService.getAllInvoices(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved successfully", response));
    }
}