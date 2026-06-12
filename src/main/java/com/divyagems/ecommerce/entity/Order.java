package com.divyagems.ecommerce.entity;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.PaymentMethodEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core order entity representing a customer purchase.
 * orderId is a human-readable reference (e.g. DG-2024-00001) distinct from the UUID PK.
 * Both shippingAddress and billingAddress are embedded snapshots to preserve
 * historical accuracy when the user later modifies their address book.
 * Razorpay fields support the payment gateway integration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_order_id", columnList = "order_id"),
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "order_status"),
        @Index(name = "idx_order_payment_status", columnList = "payment_status")
})
public class Order extends BaseEntity {

    // Human-readable order reference (DG-2024-00001)
    @Column(name = "order_id", nullable = false, unique = true, length = 30)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatusEnum orderStatus = OrderStatusEnum.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatusEnum paymentStatus = PaymentStatusEnum.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethodEnum paymentMethod;

    // ─── Financial Breakdown ──────────────────────────────────
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_charge", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingCharge = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "coupon_discount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    // ─── Address Snapshots ────────────────────────────────────
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName",   column = @Column(name = "shipping_first_name")),
            @AttributeOverride(name = "lastName",    column = @Column(name = "shipping_last_name")),
            @AttributeOverride(name = "phone",       column = @Column(name = "shipping_phone")),
            @AttributeOverride(name = "addressLine1",column = @Column(name = "shipping_address_line1")),
            @AttributeOverride(name = "addressLine2",column = @Column(name = "shipping_address_line2")),
            @AttributeOverride(name = "city",        column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "state",       column = @Column(name = "shipping_state")),
            @AttributeOverride(name = "pinCode",     column = @Column(name = "shipping_pin_code")),
            @AttributeOverride(name = "country",     column = @Column(name = "shipping_country"))
    })
    private OrderAddress shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName",   column = @Column(name = "billing_first_name")),
            @AttributeOverride(name = "lastName",    column = @Column(name = "billing_last_name")),
            @AttributeOverride(name = "phone",       column = @Column(name = "billing_phone")),
            @AttributeOverride(name = "addressLine1",column = @Column(name = "billing_address_line1")),
            @AttributeOverride(name = "addressLine2",column = @Column(name = "billing_address_line2")),
            @AttributeOverride(name = "city",        column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state",       column = @Column(name = "billing_state")),
            @AttributeOverride(name = "pinCode",     column = @Column(name = "billing_pin_code")),
            @AttributeOverride(name = "country",     column = @Column(name = "billing_country"))
    })
    private OrderAddress billingAddress;

    // ─── Razorpay ────────────────────────────────────────────
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    // ─── Shipping / Tracking ─────────────────────────────────
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipping_provider", length = 100)
    private String shippingProvider;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ─── Notes ───────────────────────────────────────────────
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // ─── Relations ────────────────────────────────────────────
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Payment payment;
}
