package com.invoiceapp.invoice.infrastructure.util;

import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    public String generateInvoiceNumber(UUID userId) {
        int currentYear = LocalDate.now().getYear();

        // This now includes deleted invoices
        Invoice lastInvoice = invoiceRepository
                .findTopByUserIdIncludingDeletedOrderByCreatedAtDesc(userId)
                .orElse(null);

        int nextSequence = 1;

        if (lastInvoice != null) {
            String lastNumber = lastInvoice.getInvoiceNumber();
            String[] parts = lastNumber.split("-");

            if (parts.length == 2) {
                int lastYear = Integer.parseInt(parts[0]);
                int lastSequence = Integer.parseInt(parts[1]);

                if (lastYear == currentYear) {
                    nextSequence = lastSequence + 1;
                }
            }
        }

        return String.format("%d-%04d", currentYear, nextSequence);
    }
}