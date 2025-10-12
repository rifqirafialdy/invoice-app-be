package com.invoiceapp.common.constants;

public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // JWT Token Expiration
    public static final int ACCESS_TOKEN_EXPIRY_MINUTES = 15;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final int EMAIL_TOKEN_EXPIRY_HOURS = 1;
    public static final int PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 1;
    public static final int PUBLIC_ACTION_TOKEN_EXPIRY_DAYS = 7;

    // Password Requirements
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

    // File Upload
    public static final long MAX_LOGO_FILE_SIZE_KB = 1024;
    public static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/jpg"
    };

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // Redis Key Prefixes
    public static final String REDIS_EMAIL_VERIFY_PREFIX = "email_verify:";
    public static final String REDIS_PASSWORD_RESET_PREFIX = "password_reset:";
    public static final String REDIS_EMAIL_CHANGE_PREFIX = "email_change:";
    public static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh_token:";
    public static final String REDIS_PUBLIC_ACTION_PREFIX = "public_action:";
    public static final String REDIS_INVOICE_SEQUENCE_PREFIX = "invoice:sequence:";
    public static final String REDIS_USER_CODE_PREFIX = "user:code:";
    public static final String REDIS_USER_CODE_SEQUENCE = "user:code:sequence";

    // Cache Names
    public static final String CACHE_INVOICES = "invoices";
    public static final String CACHE_CLIENTS = "clients";
    public static final String CACHE_PRODUCTS = "products";

    // Email Templates
    public static final String TEMPLATE_VERIFICATION_EMAIL = "verification-email";
    public static final String TEMPLATE_PASSWORD_RESET = "password-reset-email";
    public static final String TEMPLATE_EMAIL_CHANGE_VERIFICATION = "email-change-verification";
    public static final String TEMPLATE_EMAIL_CHANGE_NOTIFICATION = "email-change-notification";
    public static final String TEMPLATE_INVOICE_WITH_ACTIONS = "invoice-with-actions";
    public static final String TEMPLATE_PAYMENT_CONFIRMATION = "payment-confirmation";

    // Invoice
    public static final String INVOICE_NUMBER_FORMAT = "%s-%d-%04d";
    public static final String USER_CODE_FORMAT = "U%04d";

    // Email Subjects
    public static final String SUBJECT_EMAIL_VERIFICATION = "Verify Your Email - Invoice Management";
    public static final String SUBJECT_PASSWORD_RESET = "Password Reset Request - Invoice Management";
    public static final String SUBJECT_EMAIL_CHANGE = "Verify Your New Email Address - Invoice Management";
    public static final String SUBJECT_INVOICE_DUE = "INVOICE DUE #%s - Action Required";
    public static final String SUBJECT_PAYMENT_CONFIRMED = "Payment Confirmed - Invoice #%s";

    // Error Messages
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_CLIENT_NOT_FOUND = "Client not found";
    public static final String ERROR_PRODUCT_NOT_FOUND = "Product not found";
    public static final String ERROR_INVOICE_NOT_FOUND = "Invoice not found";
    public static final String ERROR_EMAIL_ALREADY_REGISTERED = "Email already registered";
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid credentials";
    public static final String ERROR_EMAIL_ALREADY_VERIFIED = "Email already verified";
    public static final String ERROR_PASSWORDS_DO_NOT_MATCH = "Passwords do not match";
    public static final String ERROR_INVALID_TOKEN = "Invalid or expired token";
    public static final String ERROR_FILE_TOO_LARGE = "File size exceeds the limit";
    public static final String ERROR_INVALID_FILE_TYPE = "Invalid file type";

    // Success Messages
    public static final String SUCCESS_EMAIL_VERIFIED = "Email verified successfully";
    public static final String SUCCESS_PASSWORD_RESET = "Password reset successfully";
    public static final String SUCCESS_EMAIL_CHANGED = "Email changed successfully";
    public static final String SUCCESS_PROFILE_UPDATED = "Profile updated successfully";
    public static final String SUCCESS_CLIENT_CREATED = "Client created successfully";
    public static final String SUCCESS_PRODUCT_CREATED = "Product created successfully";
    public static final String SUCCESS_INVOICE_CREATED = "Invoice created successfully";
}