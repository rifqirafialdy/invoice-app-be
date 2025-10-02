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
    public void sendInvoiceEmail(String to, String invoiceUrl, String invoiceNumber) {
        try {
            Context context = new Context();
            context.setVariable("invoiceUrl", invoiceUrl);
            context.setVariable("invoiceNumber", invoiceNumber);

            String htmlContent = templateEngine.process("invoice-email.html", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Invoice #" + invoiceNumber + " - Invoice Management");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invoice email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send invoice email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }
}