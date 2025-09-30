package com.invoiceapp.auth.application.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendInvoiceEmail(String to, String invoiceUrl, String invoiceNumber);
}