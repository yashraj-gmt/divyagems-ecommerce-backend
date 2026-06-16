package com.divyagems.ecommerce.email;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.application.name:Divya Gems}")
    private String appName;

    // ─── Public API ─────────────────────────────────────────────

    @Async("emailTaskExecutor")
    @Override
    public void sendVerificationEmail(String to, String token) {
        String verifyUrl = frontendUrl + "/auth/verify-email?token=" + token;
        Context ctx = context(Map.of("verifyUrl", verifyUrl, "frontendUrl", frontendUrl));
        String html = templateEngine.process("email/email-verification", ctx);
        sendHtmlEmail(to, appName + " — Verify Your Email Address", html);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
        Context ctx = context(Map.of("resetUrl", resetUrl, "frontendUrl", frontendUrl));
        String html = templateEngine.process("email/password-reset", ctx);
        sendHtmlEmail(to, appName + " — Reset Your Password", html);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendOrderConfirmationEmail(String to, String orderId, BigDecimal totalAmount) {
        Context ctx = context(Map.of(
                "orderId", orderId,
                "totalAmount", totalAmount.toPlainString(),
                "paymentMethod", "Online / COD",
                "frontendUrl", frontendUrl
        ));
        String html = templateEngine.process("email/order-confirmation", ctx);
        sendHtmlEmail(to, appName + " — Order Confirmed! " + orderId, html);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendPaymentConfirmationEmail(String to, String orderId) {
        Context ctx = context(Map.of(
                "orderId", orderId,
                "amount", "—",
                "razorpayPaymentId", "—",
                "frontendUrl", frontendUrl
        ));
        String html = templateEngine.process("email/payment-confirmation", ctx);
        sendHtmlEmail(to, appName + " — Payment Received for " + orderId, html);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendOrderStatusUpdateEmail(String to, String orderId, String status) {
        String emoji = statusEmoji(status);
        String label = status.replace("_", " ");
        String msg   = statusMessage(status);
        Context ctx = context(Map.of(
                "orderId", orderId,
                "statusEmoji", emoji,
                "statusLabel", label,
                "statusMessage", msg,
                "frontendUrl", frontendUrl
        ));
        String html = templateEngine.process("email/order-status-update", ctx);
        sendHtmlEmail(to, appName + " — Order Update: " + orderId, html);
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
            log.info("Email sent → {} | subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email → {} | subject: {} | error: {}", to, subject, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email → {} | error: {}", to, e.getMessage(), e);
        }
    }

    private Context context(Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return ctx;
    }

    private String statusEmoji(String status) {
        return switch (status) {
            case "SHIPPED"          -> "🚚";
            case "OUT_FOR_DELIVERY" -> "🏃";
            case "DELIVERED"        -> "📦";
            case "CANCELLED"        -> "❌";
            case "REFUND_INITIATED" -> "💰";
            case "REFUNDED"         -> "✅";
            default                 -> "🔔";
        };
    }

    private String statusMessage(String status) {
        return switch (status) {
            case "PROCESSING"       -> "Your order is being prepared for dispatch.";
            case "SHIPPED"          -> "Your order is on its way! Track it using the tracking number above.";
            case "OUT_FOR_DELIVERY" -> "Your order is out for delivery today. Please keep your phone handy.";
            case "DELIVERED"        -> "Your order has been delivered. We hope you love your gems! 🌟";
            case "CANCELLED"        -> "Your order has been cancelled. If you paid online, a refund will be initiated.";
            case "REFUND_INITIATED" -> "A refund for your order has been initiated. It may take 5–7 business days.";
            case "REFUNDED"         -> "Your refund has been processed successfully.";
            default                 -> "Your order status has been updated.";
        };
    }
}
