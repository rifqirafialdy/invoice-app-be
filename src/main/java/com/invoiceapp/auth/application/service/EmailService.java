package com.invoiceapp.auth.application.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendInvoiceEmailWithActions(String to, String clientName, String invoiceNumber, String totalAmount, String dueDate, String invoiceViewLink, String paymentLink, String cancelLink);
    void sendInvoiceCancellationNotification(String userEmail, String clientName, String invoiceNumber);
    void sendPaymentConfirmationNotification(String userEmail, String clientName, String invoiceNumber);
    void sendPaymentConfirmationEmail(String to, String clientName, String invoiceNumber,
                                      String totalAmount, String invoiceViewLink);
    void sendSimpleEmail(String to, String subject, String message);
    void sendEmailChangeVerification(String to, String token);
    void sendEmailChangeNotification(String oldEmail, String newEmail);

}