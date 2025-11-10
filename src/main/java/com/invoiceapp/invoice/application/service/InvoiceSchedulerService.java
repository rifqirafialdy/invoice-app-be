package com.invoiceapp.invoice.application.service;

import com.invoiceapp.invoice.application.helper.RecurringInvoiceHelper;
import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.entity.InvoiceItem;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import com.invoiceapp.invoice.infrastructure.util.InvoiceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager; // Import CacheManager
import org.springframework.cache.Cache; // Import Cache
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceSchedulerService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final InvoiceEmailService invoiceEmailService;
    private final RecurringInvoiceHelper recurringInvoiceHelper;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void generateRecurringInvoices() {
        LocalDate today = LocalDate.now();

        log.info("Starting recurring invoice generation...");

        List<Invoice> invoicesToGenerate = invoiceRepository
                .findByIsRecurringTrueAndNextGenerationDateLessThanEqual(today);

        int generated = 0;
        int warned = 0;
        int stoppedUnpaid = 0;

        for (Invoice source : invoicesToGenerate) {
            try {
                InvoiceStatus status = source.getStatus();
                LocalDate nextGenDate = source.getNextGenerationDate();

                if (status == InvoiceStatus.PAID) {
                    LocalDate scheduledDate = nextGenDate;
                    Invoice newInvoice = createRecurringInvoice(source, scheduledDate);

                    invoiceRepository.save(newInvoice);
                    generated++;

                    if (newInvoice.getStatus() == InvoiceStatus.SENT) {
                        invoiceEmailService.sendInvoiceActionEmail(newInvoice, "Recurring Invoice Generated");
                    }

                    source.setIsRecurring(false);
                    invoiceRepository.save(source);

                    log.info("Generated recurring invoice {} from {} (scheduled: {})",
                            newInvoice.getInvoiceNumber(),
                            source.getInvoiceNumber(),
                            scheduledDate);
                    continue;
                }

                if (nextGenDate.isEqual(today)) {
                    invoiceEmailService.sendPaymentUrgentWarningToClient(source);
                    invoiceEmailService.sendRecurringWarningToUser(source);
                    warned++;

                    log.warn("Grace period for invoice {} - client warned, checking again tomorrow",
                            source.getInvoiceNumber());
                    continue;
                }

                if (nextGenDate.isBefore(today)) {
                    source.setIsRecurring(false);
                    invoiceRepository.save(source);
                    stoppedUnpaid++;

                    invoiceEmailService.sendRecurringStoppedToUser(source);
                    invoiceEmailService.sendRecurringStoppedToClient(source);

                    log.warn("Stopped recurring for invoice {} - status is {} and grace period expired",
                            source.getInvoiceNumber(), status);
                    continue;
                }

            } catch (Exception e) {
                log.error("Failed to process recurring invoice {}: {}",
                        source.getInvoiceNumber(), e.getMessage(), e);
            }
        } //

        log.info("Recurring generation completed. Generated: {}, Warned: {}, Stopped: {}",
                generated, warned, stoppedUnpaid);

        if (generated > 0 || stoppedUnpaid > 0) {
            clearInvoicesCache();
        }
    }


    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void updateInvoiceStatuses() {
        log.info("Starting scheduled invoice status update...");

        LocalDate today = LocalDate.now();
        int dueCount = 0;
        int overdueCount = 0;

        List<Invoice> sentInvoices = invoiceRepository.findByStatus(InvoiceStatus.SENT);
        for (Invoice invoice : sentInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                overdueCount++;
                invoiceEmailService.sendOverdueReminderEmail(invoice);
            } else if (invoice.getDueDate().isEqual(today)) {
                invoice.setStatus(InvoiceStatus.DUE);
                dueCount++;
                invoiceEmailService.sendDueReminderEmail(invoice);
            }
        }
        invoiceRepository.saveAll(sentInvoices);

        List<Invoice> dueInvoices = invoiceRepository.findByStatus(InvoiceStatus.DUE);
        for (Invoice invoice : dueInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                overdueCount++;
                invoiceEmailService.sendOverdueReminderEmail(invoice);
            }
        }
        invoiceRepository.saveAll(dueInvoices);

        boolean changesMade = dueCount > 0 || overdueCount > 0;

        if (changesMade) {
            log.info("Invoice status update completed. Updated to DUE: {}, Updated to OVERDUE: {}", dueCount, overdueCount);
            clearInvoicesCache();
        } else {
            log.info("No invoice statuses required an update.");
        }
    }


    private void clearInvoicesCache() {
        Cache invoiceCache = cacheManager.getCache("invoices");
        if (invoiceCache != null) {
            invoiceCache.clear();
            log.info("Cleared 'invoices' cache due to scheduled updates.");
        }
    }

    private Invoice createRecurringInvoice(Invoice source, LocalDate scheduledIssueDate) {
        Invoice newInvoice = new Invoice();

        newInvoice.setUser(source.getUser());
        newInvoice.setClient(source.getClient());
        newInvoice.setIssueDate(scheduledIssueDate);

        long daysBetween = ChronoUnit.DAYS.between(source.getIssueDate(), source.getDueDate());
        newInvoice.setDueDate(scheduledIssueDate.plusDays(daysBetween));

        newInvoice.setIsRecurring(true);
        newInvoice.setRecurringFrequency(source.getRecurringFrequency());
        newInvoice.setNextGenerationDate(
                recurringInvoiceHelper.calculateNextGenerationDate(
                        scheduledIssueDate,
                        source.getRecurringFrequency()
                )
        );

        newInvoice.setRecurringSeriesId(
                source.getRecurringSeriesId() != null ?
                        source.getRecurringSeriesId() : source.getId()
        );

        newInvoice.setStatus(InvoiceStatus.SENT);
        newInvoice.setSubtotal(source.getSubtotal());
        newInvoice.setTaxRate(source.getTaxRate());
        newInvoice.setTaxAmount(source.getTaxAmount());
        newInvoice.setTotal(source.getTotal());
        newInvoice.setNotes(source.getNotes());

        newInvoice.setInvoiceNumber(
                invoiceNumberGenerator.generateInvoiceNumber(source.getUser().getId())
        );

        for (InvoiceItem sourceItem : source.getItems()) {
            InvoiceItem newItem = InvoiceItem.builder()
                    .product(sourceItem.getProduct())
                    .productName(sourceItem.getProductName())
                    .productDescription(sourceItem.getProductDescription())
                    .quantity(sourceItem.getQuantity())
                    .unitPrice(sourceItem.getUnitPrice())
                    .build();
            newInvoice.addItem(newItem);
        }

        newInvoice.calculateTotals();

        return newInvoice;
    }
}

