// File: com/invoiceapp/invoice/application/service/PublicInvoiceService.java (BARU)

package com.invoiceapp.invoice.application.service;

import java.util.UUID;

public interface PublicInvoiceService {
    void requestInvoiceCancellation(String token);
    void confirmPaymentReceived(String token);

}