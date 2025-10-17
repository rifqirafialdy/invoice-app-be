package com.invoiceapp.invoice.application.service;

import com.invoiceapp.auth.application.service.EmailService;
import com.invoiceapp.auth.application.service.TokenService;
import com.invoiceapp.invoice.domain.entity.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEmailService {

    private final EmailService emailService;
    private final TokenService tokenService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void sendInvoiceActionEmail(Invoice invoice) {
        sendInvoiceActionEmail(invoice, "New Invoice");
    }

    public void sendInvoiceActionEmail(Invoice invoice, String subjectPrefix) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            log.warn("Skipping {} email for Invoice {} - Client has no email",
                    subjectPrefix, invoice.getInvoiceNumber());
            return;
        }

        try {
            String viewToken = tokenService.generatePublicActionToken(invoice.getId(), "VIEW");
            String payToken = tokenService.generatePublicActionToken(invoice.getId(), "PAY");
            String cancelToken = tokenService.generatePublicActionToken(invoice.getId(), "CANCEL");

            String invoiceViewLink = String.format("%s/invoices/view?token=%s",
                    frontendUrl, viewToken);
            String paymentLink = String.format("%s/invoices/pay?token=%s",
                    frontendUrl, payToken);
            String cancelLink = String.format("%s/invoices/cancel-request?token=%s",
                    frontendUrl, cancelToken);

            String formattedTotal = formatCurrency(invoice.getTotal());

            emailService.sendInvoiceEmailWithActions(
                    clientEmail,
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    formattedTotal,
                    invoice.getDueDate().toString(),
                    invoiceViewLink,
                    paymentLink,
                    cancelLink
            );

            log.info("{} email sent for Invoice {} to {}",
                    subjectPrefix, invoice.getInvoiceNumber(), clientEmail);

        } catch (Exception e) {
            log.error("Failed to send {} email for Invoice {}: {}",
                    subjectPrefix, invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    public void sendDueReminderEmail(Invoice invoice) {
        sendInvoiceActionEmail(invoice, "Reminder: Invoice DUE Today");
    }

    public void sendOverdueReminderEmail(Invoice invoice) {
        sendInvoiceActionEmail(invoice, "Urgent: Invoice OVERDUE");
    }

    public void sendPaymentConfirmationEmail(Invoice invoice) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            log.warn("Skipping payment confirmation for Invoice {} - Client has no email",
                    invoice.getInvoiceNumber());
            return;
        }

        try {
            String invoiceViewLink = String.format("%s/invoices/view/%s",
                    frontendUrl, invoice.getInvoiceNumber());
            String formattedTotal = formatCurrency(invoice.getTotal());

            emailService.sendPaymentConfirmationEmail(
                    clientEmail,
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    formattedTotal,
                    invoiceViewLink
            );

            log.info("Payment confirmation sent for Invoice {} to {}",
                    invoice.getInvoiceNumber(), clientEmail);

        } catch (Exception e) {
            log.error("Failed to send payment confirmation for Invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    public void sendPaymentUrgentWarningToClient(Invoice invoice) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            log.warn("Cannot send warning - client has no email for invoice {}",
                    invoice.getInvoiceNumber());
            return;
        }

        try {
            String payToken = tokenService.generatePublicActionToken(invoice.getId(), "PAY");
            String paymentLink = String.format("%s/invoices/pay?token=%s", frontendUrl, payToken);
            String invoiceViewLink = String.format("%s/invoices/view/%s",
                    frontendUrl, invoice.getInvoiceNumber());


            String subject = "⚠️ URGENT: Payment Required - Recurring Will Stop";
            String message = String.format(
                    "Dear %s,\n\n" +
                            "⚠️ URGENT ACTION REQUIRED\n\n" +
                            "Your recurring invoice #%s is OVERDUE and unpaid.\n\n" +
                            "Amount Due: Rp %s\n" +
                            "Due Date: %s\n" +
                            "Status: %s\n\n" +
                            "⏰ You have until TOMORROW to make payment.\n\n" +
                            "If payment is not received by tomorrow, your recurring invoice service will be automatically stopped " +
                            "and no future invoices will be generated.\n\n" +
                            "Pay Now: %s\n" +
                            "View Invoice: %s\n\n" +
                            "Please contact us immediately if you have any questions.\n\n" +
                            "Thank you,\n" +
                            "%s",
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getDueDate(),
                    invoice.getStatus(),
                    paymentLink,
                    invoiceViewLink,
                    invoice.getUser().getName() != null ? invoice.getUser().getName() : "Invoice Management"
            );

            emailService.sendSimpleEmail(clientEmail, subject, message);

            log.info("Sent urgent payment warning to client {} for invoice {}",
                    clientEmail, invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("Failed to send urgent warning to client: {}", e.getMessage());
        }
    }

    public void sendRecurringWarningToUser(Invoice invoice) {
        String userEmail = invoice.getUser().getEmail();

        if (userEmail == null || userEmail.isBlank()) {
            return;
        }

        try {
            String subject = "[WARNING] Recurring Payment Not Received - Invoice #" + invoice.getInvoiceNumber();
            String message = String.format(
                    "⚠️ PAYMENT WARNING\n\n" +
                            "Client %s has not paid Invoice #%s.\n\n" +
                            "Details:\n" +
                            "- Amount: Rp %s\n" +
                            "- Due Date: %s\n" +
                            "- Status: %s\n" +
                            "- Next Generation Date: TODAY\n\n" +
                            "Action Taken:\n" +
                            "✓ Urgent warning email sent to client\n" +
                            "✓ Recurring generation postponed for 1 day\n\n" +
                            "If payment is not received by tomorrow, recurring will be automatically stopped.\n\n" +
                            "You may want to contact the client directly.",
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getDueDate(),
                    invoice.getStatus()
            );

            emailService.sendSimpleEmail(userEmail, subject, message);

        } catch (Exception e) {
            log.error("Failed to send warning to user: {}", e.getMessage());
        }
    }


    public void sendRecurringStoppedToUser(Invoice invoice) {
        String userEmail = invoice.getUser().getEmail();

        if (userEmail == null || userEmail.isBlank()) {
            return;
        }

        try {
            String subject = "[RECURRING STOPPED] Invoice #" + invoice.getInvoiceNumber();
            String message = String.format(
                    "🛑 RECURRING STOPPED\n\n" +
                            "Recurring invoice generation has been stopped for:\n\n" +
                            "Invoice #%s\n" +
                            "Client: %s\n" +
                            "Amount: Rp %s\n" +
                            "Status: %s\n\n" +
                            "Reason: Payment not received after grace period.\n\n" +
                            "No future invoices will be generated for this recurring series.\n\n" +
                            "You can manually restart recurring after receiving payment.",
                    invoice.getInvoiceNumber(),
                    invoice.getClient().getName(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getStatus()
            );

            emailService.sendSimpleEmail(userEmail, subject, message);

        } catch (Exception e) {
            log.error("Failed to send stopped notification to user: {}", e.getMessage());
        }
    }

    public void sendRecurringStoppedToClient(Invoice invoice) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            return;
        }

        try {
            String subject = "🛑 Recurring Service Stopped - Invoice #" + invoice.getInvoiceNumber();
            String message = String.format(
                    "Dear %s,\n\n" +
                            "Your recurring invoice service has been stopped due to non-payment.\n\n" +
                            "Unpaid Invoice: #%s\n" +
                            "Amount: Rp %s\n" +
                            "Status: %s\n\n" +
                            "No future invoices will be generated until this is resolved.\n\n" +
                            "To restart service:\n" +
                            "1. Pay the outstanding invoice\n" +
                            "2. Contact us to reactivate recurring invoices\n\n" +
                            "Thank you,\n" +
                            "%s",
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getStatus(),
                    invoice.getUser().getName() != null ? invoice.getUser().getName() : "Invoice Management"
            );

            emailService.sendSimpleEmail(clientEmail, subject, message);

        } catch (Exception e) {
            log.error("Failed to send stopped notification to client: {}", e.getMessage());
        }
    }
    public void sendCancellationApprovedEmail(Invoice invoice) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            log.warn("Skipping cancellation approved email for Invoice {} - Client has no email",
                    invoice.getInvoiceNumber());
            return;
        }

        try {
            String subject = "✓ Cancellation Approved - Invoice #" + invoice.getInvoiceNumber();
            String message = String.format(
                    "Dear %s,\n\n" +
                            "Your cancellation request for Invoice #%s has been approved.\n\n" +
                            "The invoice has been cancelled and no further action is required from you.\n\n" +
                            "Invoice Details:\n" +
                            "- Invoice Number: %s\n" +
                            "- Amount: Rp %s\n" +
                            "- Status: CANCELLED\n\n" +
                            "If you have any questions, please contact us.\n\n" +
                            "Thank you,\n" +
                            "%s",
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceNumber(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getUser().getName() != null ? invoice.getUser().getName() : "Invoice Management"
            );

            emailService.sendSimpleEmail(clientEmail, subject, message);

            log.info("Cancellation approved email sent for Invoice {} to {}",
                    invoice.getInvoiceNumber(), clientEmail);

        } catch (Exception e) {
            log.error("Failed to send cancellation approved email for Invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    public void sendCancellationRejectedEmail(Invoice invoice) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            log.warn("Skipping cancellation rejected email for Invoice {} - Client has no email",
                    invoice.getInvoiceNumber());
            return;
        }

        try {
            String subject = "✗ Cancellation Request Rejected - Invoice #" + invoice.getInvoiceNumber();
            String message = String.format(
                    "Dear %s,\n\n" +
                            "Your cancellation request for Invoice #%s has been rejected.\n\n" +
                            "The invoice remains active and payment is still due.\n\n" +
                            "Invoice Details:\n" +
                            "- Invoice Number: %s\n" +
                            "- Amount Due: Rp %s\n" +
                            "- Due Date: %s\n" +
                            "- Status: %s\n\n" +
                            "If you have any questions or concerns, please contact us.\n\n" +
                            "Thank you,\n" +
                            "%s",
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceNumber(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getDueDate(),
                    invoice.getStatus(),
                    invoice.getUser().getName() != null ? invoice.getUser().getName() : "Invoice Management"
            );

            emailService.sendSimpleEmail(clientEmail, subject, message);

            log.info("Cancellation rejected email sent for Invoice {} to {}",
                    invoice.getInvoiceNumber(), clientEmail);

        } catch (Exception e) {
            log.error("Failed to send cancellation rejected email for Invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    public void sendPaymentRejectedEmail(Invoice invoice) {
        String clientEmail = invoice.getClient().getEmail();

        if (clientEmail == null || clientEmail.isBlank()) {
            log.warn("Skipping payment rejected email for Invoice {} - Client has no email",
                    invoice.getInvoiceNumber());
            return;
        }

        try {
            String payToken = tokenService.generatePublicActionToken(invoice.getId(), "PAY");
            String paymentLink = String.format("%s/invoices/pay?token=%s", frontendUrl, payToken);

            String subject = "✗ Payment Not Verified - Invoice #" + invoice.getInvoiceNumber();
            String message = String.format(
                    "Dear %s,\n\n" +
                            "We were unable to verify your payment for Invoice #%s.\n\n" +
                            "Reason: Payment not received in our account\n\n" +
                            "Invoice Details:\n" +
                            "- Invoice Number: %s\n" +
                            "- Amount Due: Rp %s\n" +
                            "- Due Date: %s\n" +
                            "- Status: %s\n\n" +
                            "Please check your payment details and try again:\n" +
                            "%s\n\n" +
                            "If you believe you have already paid, please contact us with your payment proof.\n\n" +
                            "Thank you,\n" +
                            "%s",
                    invoice.getClient().getName(),
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceNumber(),
                    formatCurrency(invoice.getTotal()),
                    invoice.getDueDate(),
                    invoice.getStatus(),
                    paymentLink,
                    invoice.getUser().getName() != null ? invoice.getUser().getName() : "Invoice Management"
            );

            emailService.sendSimpleEmail(clientEmail, subject, message);

            log.info("Payment rejected email sent for Invoice {} to {}",
                    invoice.getInvoiceNumber(), clientEmail);

        } catch (Exception e) {
            log.error("Failed to send payment rejected email for Invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    private String formatCurrency(BigDecimal amount) {
        Locale indonesia = new Locale("in", "ID");
        NumberFormat formatter = NumberFormat.getNumberInstance(indonesia);
        return formatter.format(amount.longValue());
    }

}