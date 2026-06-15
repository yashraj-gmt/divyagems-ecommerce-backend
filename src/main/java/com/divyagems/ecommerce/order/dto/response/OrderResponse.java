package com.divyagems.ecommerce.order.dto.response;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.PaymentMethodEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order detail response returned after placeOrder, verifyPayment,
 * getOrderById, and status updates.
 *
 * razorpayOrderId and razorpayKeyId are only populated for ONLINE payment orders
 * that are still in PENDING payment status — i.e. awaiting payment from the frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String orderId;
    private OrderStatusEnum status;
    private PaymentStatusEnum paymentStatus;
    private PaymentMethodEnum paymentMethod;

    private List<OrderItemResponse> items;

    private OrderAddressResponse shippingAddress;
    private OrderAddressResponse billingAddress;

    // ─── Financial breakdown ──────────────────────────────
    private BigDecimal subtotal;
    private BigDecimal shippingCharge;
    private BigDecimal discountAmount;
    private String couponCode;
    private BigDecimal couponDiscount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    // ─── Razorpay (ONLINE payments only) ─────────────────
    /** Razorpay order ID to pass to the checkout widget. Null for COD. */
    private String razorpayOrderId;

    /** Razorpay publishable key — needed by the frontend checkout widget. */
    private String razorpayKeyId;

    // ─── Shipping / tracking ──────────────────────────────
    private String trackingNumber;
    private String shippingProvider;
    private LocalDate estimatedDelivery;

    // ─── Status timeline ──────────────────────────────────
    private List<OrderStatusHistoryResponse> statusHistory;

    private LocalDateTime createdAt;
}
