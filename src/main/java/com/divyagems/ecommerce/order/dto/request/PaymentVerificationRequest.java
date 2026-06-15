package com.divyagems.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request body for POST /api/orders/verify-payment.
 * Contains all three identifiers returned by the Razorpay checkout widget
 * plus our own orderId for reconciliation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationRequest {

    /** Our human-readable order reference (e.g. DG-202601-00001). */
    @NotBlank(message = "orderId is required")
    private String orderId;

    /** Razorpay's order ID (rzp_order_xxx). */
    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    /** Razorpay's payment ID after successful capture (pay_xxx). */
    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    /** HMAC-SHA256 signature from Razorpay checkout. */
    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}
