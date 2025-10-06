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
import java.time.temporal.ChronoUnit;
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
        LocalDate today = LocalDate.now();

        List<Invoice> invoicesToGenerate = invoiceRepository
                .findByIsRecurringTrueAndNextGenerationDateLessThanEqual(today);

        int generated = 0;

        for (Invoice template : invoicesToGenerate) {
            try {
                LocalDate scheduledDate = template.getNextGenerationDate();
                Invoice newInvoice = createRecurringInvoice(template, scheduledDate);

                invoiceRepository.save(newInvoice);

                LocalDate nextDate = calculateNextDate(scheduledDate, template.getRecurringFrequency());
                template.setNextGenerationDate(nextDate);
                invoiceRepository.save(template);

                generated++;
                log.info("Generated recurring invoice {} from template {} (scheduled: {})",
                        newInvoice.getInvoiceNumber(),
                        template.getInvoiceNumber(),
                        scheduledDate);
            } catch (Exception e) {
                log.error("Failed to generate recurring invoice from {}: {}",
                        template.getInvoiceNumber(), e.getMessage());
            }
        }

        log.info("Recurring invoice generation completed. Generated: {}", generated);
    }

    private Invoice createRecurringInvoice(Invoice template, LocalDate scheduledIssueDate) {
        Invoice newInvoice = new Invoice();
        newInvoice.setUser(template.getUser());
        newInvoice.setClient(template.getClient());

        newInvoice.setIssueDate(scheduledIssueDate);

        LocalDate dueDate = scheduledIssueDate.plusDays(
                ChronoUnit.DAYS.between(template.getIssueDate(), template.getDueDate())
        );
        newInvoice.setDueDate(dueDate);

        newInvoice.setIsRecurring(false);
        newInvoice.setStatus(InvoiceStatus.SENT);
        newInvoice.setSubtotal(template.getSubtotal());
        newInvoice.setTaxRate(template.getTaxRate());
        newInvoice.setTaxAmount(template.getTaxAmount());
        newInvoice.setTotal(template.getTotal());
        newInvoice.setNotes(template.getNotes());

        newInvoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber(template.getUser().getId()));

        return newInvoice;
    }

    private LocalDate calculateNextDate(LocalDate from, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "DAILY" -> from.plusDays(1);
            case "WEEKLY" -> from.plusWeeks(1);
            case "MONTHLY" -> from.plusMonths(1);
            case "YEARLY" -> from.plusYears(1);
            default -> from.plusMonths(1);
        };
    }
}