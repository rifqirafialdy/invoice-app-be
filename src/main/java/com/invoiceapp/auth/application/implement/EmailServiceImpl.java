package com.invoiceapp.auth.application.implement;

import com.invoiceapp.auth.application.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            String verificationUrl = frontendUrl + "/verify-email/" + token;

            Context context = new Context();
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("email", to);

            String htmlContent = templateEngine.process("verification-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify Your Email - Invoice Management");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("email", to);

            String htmlContent = templateEngine.process("password-reset-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Password Reset Request - Invoice Management");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendInvoiceEmailWithActions(String to, String clientName, String invoiceNumber, String totalAmount, String dueDate, String invoiceViewLink, String paymentLink, String cancelLink) {
        try {
            Context context = new Context();
            context.setVariable("clientName", clientName);
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("dueDate", dueDate);
            context.setVariable("invoiceViewLink", invoiceViewLink);
            context.setVariable("paymentLink", paymentLink);
            context.setVariable("cancelLink", cancelLink);

            String htmlContent = templateEngine.process("invoice-with-actions", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("INVOICE DUE #" + invoiceNumber + " - Action Required");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invoice with actions email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send invoice email with actions to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendInvoiceCancellationNotification(String userEmail, String clientName, String invoiceNumber) {
        try {
            String subject = "[ACTION REQUIRED] Cancellation Request for Invoice #" + invoiceNumber;
            String body = String.format("Client %s has requested a cancellation for Invoice #%s. " +
                            "Please log in to the application to review and confirm the status change.",
                    clientName, invoiceNumber);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("Cancellation notification sent to user: {}", userEmail);
        } catch (MessagingException e) {
            log.error("Failed to send cancellation notification email to: {}", userEmail, e);
            throw new RuntimeException("Failed to send notification email");
        }
    }

    @Override
    public void sendPaymentConfirmationNotification(String userEmail, String clientName, String invoiceNumber) {
        try {
            String subject = "[ACTION REQUIRED] Payment Confirmation for Invoice #" + invoiceNumber;
            String body = String.format("Client %s has confirmed payment for Invoice #%s. " +
                            "Please verify your bank statement and manually update the invoice status to PAID.",
                    clientName, invoiceNumber);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("Payment confirmation notification sent to user: {}", userEmail);
        } catch (MessagingException e) {
            log.error("Failed to send payment confirmation notification email to: {}", userEmail, e);
            throw new RuntimeException("Failed to send notification email");
        }
    }

    @Override
    public void sendPaymentConfirmationEmail(String to, String clientName, String invoiceNumber,
                                             String totalAmount, String invoiceViewLink) {
        try {
            Context context = new Context();
            context.setVariable("clientName", clientName);
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("invoiceViewLink", invoiceViewLink);

            String htmlContent = templateEngine.process("payment-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Payment Confirmed - Invoice #" + invoiceNumber);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Payment confirmation email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send payment confirmation email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(message, false);

            mailSender.send(mimeMessage);
            log.info("Simple email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendEmailChangeVerification(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify Your Email Change Request - Invoice Management");

            String verifyUrl = frontendUrl + "/verify-email-change?token=" + token;

            Context context = new Context();
            context.setVariable("verifyUrl", verifyUrl);

            String htmlContent = templateEngine.process("email-change-verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email change verification sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email change verification to: {}", to, e);
            throw new RuntimeException("Failed to send email change verification");
        }
    }

    @Override
    public void sendEmailChangeNotification(String newEmail, String oldEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(newEmail);
            helper.setSubject("Your Email Has Been Changed - Invoice Management");

            Context context = new Context();
            context.setVariable("oldEmail", oldEmail);

            String htmlContent = templateEngine.process("email-change-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email change notification sent to new email: {}", newEmail);

        } catch (MessagingException e) {
            log.error("Failed to send email change notification to: {}", newEmail, e);
            throw new RuntimeException("Failed to send email change notification");
        }
    }
}