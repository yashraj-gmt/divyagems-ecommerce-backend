package com.divyagems.ecommerce.email;

import java.math.BigDecimal;

/**
 * Contract for sending transactional emails.
 * All implementations must be asynchronous to avoid blocking the request thread.
 */
public interface EmailService {

    /**
     * Send an email verification link to a newly registered user.
     *
     * @param to    recipient email address
     * @param token email verification UUID token
     */
    void sendVerificationEmail(String to, String token);

    /**
     * Send a password reset link to a user who requested it.
     *
     * @param to    recipient email address
     * @param token password reset UUID token (15-min expiry)
     */
    void sendPasswordResetEmail(String to, String token);

    /**
     * Send an order confirmation email after a successful order placement.
     *
     * @param to          recipient email address
     * @param orderId     human-readable order ID (e.g. DG-202601-00001)
     * @param totalAmount total order amount in INR
     */
    void sendOrderConfirmationEmail(String to, String orderId, BigDecimal totalAmount);

    /**
     * Send a payment success confirmation email after Razorpay payment verification.
     *
     * @param to      recipient email address
     * @param orderId human-readable order ID
     */
    void sendPaymentConfirmationEmail(String to, String orderId);

    /**
     * Send an order status update notification (e.g. SHIPPED, DELIVERED).
     *
     * @param to      recipient email address
     * @param orderId human-readable order ID
     * @param status  new order status string
     */
    void sendOrderStatusUpdateEmail(String to, String orderId, String status);
}

