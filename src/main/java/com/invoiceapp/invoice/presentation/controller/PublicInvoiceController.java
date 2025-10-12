package com.invoiceapp.invoice.presentation.controller;

import com.invoiceapp.common.dto.ApiResponse;
import com.invoiceapp.invoice.application.service.PublicInvoiceService;
import com.invoiceapp.invoice.presentation.dto.response.PublicInvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/invoice")
@RequiredArgsConstructor
public class PublicInvoiceController {

    private final PublicInvoiceService publicInvoiceService;
    @PostMapping("/request-cancel")
    public ResponseEntity<ApiResponse<Void>> requestCancellation(@RequestParam("token") String token) {
        publicInvoiceService.requestInvoiceCancellation(token);

        return ResponseEntity.ok(ApiResponse.success("Cancellation request submitted. Owner will be notified for approval.", null));
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(@RequestParam("token") String token) {
        publicInvoiceService.confirmPaymentReceived(token);

        return ResponseEntity.ok(ApiResponse.success("Payment confirmation received. The payment is pending manual verification.", null));
    }
    @GetMapping("/view")
    public ResponseEntity<ApiResponse<PublicInvoiceResponse>> viewInvoice(@RequestParam("token") String token) {
        PublicInvoiceResponse response = publicInvoiceService.viewInvoice(token);
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved successfully", response));
    }

}