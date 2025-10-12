
package com.invoiceapp.invoice.application.service;

import com.invoiceapp.invoice.presentation.dto.response.PublicInvoiceResponse;


public interface PublicInvoiceService {
    void requestInvoiceCancellation(String token);
    void confirmPaymentReceived(String token);
    PublicInvoiceResponse viewInvoice(String token);


}