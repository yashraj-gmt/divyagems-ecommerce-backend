package com.divyagems.ecommerce.entity;

import com.divyagems.ecommerce.enums.GatewayNameEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment record linked 1:1 with an Order.
 * gatewayResponse stores the raw JSON from Razorpay/COD for audit purposes.
 * refundAmount/refundedAt are populated when a refund is processed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_payment_order_id", columnList = "order_id")
})
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "transaction_id", unique = true, length = 150)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_name", nullable = false, length = 30)
    private GatewayNameEnum gatewayName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatusEnum status = PaymentStatusEnum.PENDING;

    // Raw JSON response from gateway for audit/debugging
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;
}
