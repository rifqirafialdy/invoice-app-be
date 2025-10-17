package com.invoiceapp.invoice.application.helper;

import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.domain.enums.RecurringFrequency;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RecurringInvoiceHelper {

    public LocalDate calculateNextGenerationDate(LocalDate issueDate, RecurringFrequency frequency) {
        if (frequency == null) {
            return null;
        }

        return frequency.calculateNextDate(issueDate);
    }

    public boolean canGenerateNext(InvoiceStatus status) {
        return status == InvoiceStatus.PAID ;
    }
}