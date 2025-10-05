package com.invoiceapp.invoice.application.service;

import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.entity.InvoiceItem;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import com.invoiceapp.invoice.infrastructure.util.InvoiceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceSchedulerService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;  // Add this

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void updateInvoiceStatuses() {
        log.info("Starting scheduled invoice status update...");

        LocalDate today = LocalDate.now();

        List<Invoice> sentInvoices = invoiceRepository.findByStatus(InvoiceStatus.SENT);

        int dueCount = 0;
        int overdueCount = 0;

        for (Invoice invoice : sentInvoices) {
            if (invoice.getDueDate().isEqual(today)) {
                invoice.setStatus(InvoiceStatus.DUE);
                dueCount++;
            } else if (invoice.getDueDate().isBefore(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                overdueCount++;
            }
        }

        invoiceRepository.saveAll(sentInvoices);

        log.info("Invoice status update completed. DUE: {}, OVERDUE: {}", dueCount, overdueCount);
    }


        @Scheduled(cron = "0 * * * * ?")
        @Transactional
        public void generateRecurringInvoices() {
            log.info("Starting recurring invoice generation...");

            LocalDate today = LocalDate.now();
            List<Invoice> recurringInvoices = invoiceRepository
                    .findByIsRecurringTrueAndNextGenerationDate(today);

            int generatedCount = 0;

            for (Invoice originalInvoice : recurringInvoices) {
                String newInvoiceNumber = invoiceNumberGenerator.generateInvoiceNumber(
                        originalInvoice.getUser().getId()
                );
                Invoice newInvoice = Invoice.builder()
                        .user(originalInvoice.getUser())
                        .client(originalInvoice.getClient())
                        .invoiceNumber(newInvoiceNumber)
                        .issueDate(today)
                        .dueDate(calculateDueDate(today, originalInvoice.getRecurringFrequency()))
                        .status(InvoiceStatus.DRAFT)
                        .taxRate(originalInvoice.getTaxRate())
                        .notes(originalInvoice.getNotes())
                        .isRecurring(false)
                        .build();
                for (InvoiceItem item : originalInvoice.getItems()) {
                    InvoiceItem newItem = InvoiceItem.builder()
                            .product(item.getProduct())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .build();
                    newInvoice.addItem(newItem);
                }

                newInvoice.getItems().forEach(item -> {
                    if (item.getTotal() == null) {
                        item.setTotal(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
                    }
                });

                newInvoice.calculateTotals();
                invoiceRepository.save(newInvoice);

                originalInvoice.setNextGenerationDate(
                        calculateNextGenerationDate(today, originalInvoice.getRecurringFrequency())
                );

                generatedCount++;
            }

            log.info("Recurring invoice generation completed. Generated: {}", generatedCount);
        }

    private LocalDate calculateDueDate(LocalDate issueDate, String frequency) {
        return switch (frequency) {
            case "WEEKLY" -> issueDate.plusWeeks(1);
            case "MONTHLY" -> issueDate.plusMonths(1);
            case "YEARLY" -> issueDate.plusYears(1);
            default -> issueDate.plusMonths(1);
        };
    }

    private LocalDate calculateNextGenerationDate(LocalDate current, String frequency) {
        return switch (frequency) {
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> current.plusMonths(1);
            case "YEARLY" -> current.plusYears(1);
            default -> current.plusMonths(1);
        };
    }

}