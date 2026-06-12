package com.divyagems.ecommerce.email;

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
}
