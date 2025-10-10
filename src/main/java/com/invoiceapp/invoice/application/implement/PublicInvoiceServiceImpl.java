package com.invoiceapp.invoice.application.implement;

import com.invoiceapp.auth.application.service.EmailService;
import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.invoice.application.service.PublicInvoiceService;
import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import com.invoiceapp.common.exception.BadRequestException;
import com.invoiceapp.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicInvoiceServiceImpl implements PublicInvoiceService {

    private final TokenService tokenService;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final CacheManager cacheManager;

    private Invoice findAndCheckInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));

        if (invoice.getStatus() == InvoiceStatus.PAID ||
                invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Invoice is already settled.");
        }
        return invoice;
    }

    @Override
    public void requestInvoiceCancellation(String token) {
        UUID invoiceId = tokenService.verifyPublicActionToken(token, "CANCEL");

        Invoice invoice = findAndCheckInvoice(invoiceId);
        invoice.setStatus(InvoiceStatus.CANCELLATION_REQUESTED);
        invoiceRepository.save(invoice);

        evictInvoiceCacheForUser(invoice.getUser().getId());

        String userEmail = invoice.getUser().getEmail();
        String clientName = invoice.getClient().getName();

        emailService.sendInvoiceCancellationNotification(userEmail, clientName, invoice.getInvoiceNumber());
    }

    @Override
    public void confirmPaymentReceived(String token) {
        UUID invoiceId = tokenService.verifyPublicActionToken(token, "PAY");

        Invoice invoice = findAndCheckInvoice(invoiceId);
        invoice.setStatus(InvoiceStatus.PAYMENT_PENDING);
        invoiceRepository.save(invoice);

        User owner = invoice.getUser();

        evictInvoiceCacheForUser(owner.getId());

        String clientName = invoice.getClient().getName();

        emailService.sendPaymentConfirmationNotification(
                owner.getEmail(),
                clientName,
                invoice.getInvoiceNumber()
        );
    }

    private void evictInvoiceCacheForUser(UUID userId) {
        Cache invoiceCache = cacheManager.getCache("invoices");
        if (invoiceCache != null) {
            invoiceCache.evict(userId.toString());
        }
    }
}
