package com.divyagems.ecommerce.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async email service implementation using Spring's JavaMailSender.
 * All methods run in a separate thread pool managed by Spring's @Async executor
 * (configured in AsyncConfig). Failures are logged but not propagated to avoid
 * disrupting the caller's transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.application.name:Divya Gems}")
    private String appName;

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        String verifyUrl = frontendUrl + "/auth/verify-email?token=" + token;
        String subject = appName + " - Verify Your Email Address";
        String body = buildVerificationEmailBody(to, verifyUrl);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
        String subject = appName + " - Reset Your Password";
        String body = buildPasswordResetEmailBody(to, resetUrl);
        sendHtmlEmail(to, subject, body);
    }

    // ─── Private Helpers ───────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} | subject: {} | error: {}", to, subject, e.getMessage());
        }
    }

    private String buildVerificationEmailBody(String to, String verifyUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; padding: 40px;">
                    <h2 style="color: #7B2D8B;">Welcome to %s 🙏</h2>
                    <p>Thank you for registering! Please verify your email address by clicking the button below.</p>
                    <p>This link expires in <strong>24 hours</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; background:#7B2D8B; color:white; padding:12px 24px;
                              border-radius:6px; text-decoration:none; font-size:16px;">
                      Verify Email Address
                    </a>
                    <p style="color: #888; margin-top: 30px;">If you did not create an account, please ignore this email.</p>
                  </div>
                </body>
                </html>
                """.formatted(appName, verifyUrl);
    }

    private String buildPasswordResetEmailBody(String to, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; padding: 40px;">
                    <h2 style="color: #7B2D8B;">Reset Your Password</h2>
                    <p>We received a request to reset your password for your %s account.</p>
                    <p>Click the button below. This link expires in <strong>15 minutes</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; background:#7B2D8B; color:white; padding:12px 24px;
                              border-radius:6px; text-decoration:none; font-size:16px;">
                      Reset Password
                    </a>
                    <p style="color: #888; margin-top: 30px;">If you did not request a password reset, please ignore this email. Your account is safe.</p>
                  </div>
                </body>
                </html>
                """.formatted(appName, resetUrl);
    }
}
