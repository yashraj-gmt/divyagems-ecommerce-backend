package com.divyagems.ecommerce.order.dto.response;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight order summary for list/card views.
 * Avoids loading full item and address data for performance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryResponse {

    private String orderId;
    private OrderStatusEnum status;
    private PaymentStatusEnum paymentStatus;
    private BigDecimal totalAmount;

    /** Total number of line items in the order. */
    private int itemCount;

    /** Image URL of the first item — used for thumbnail display in order list. */
    private String primaryImageUrl;

    private LocalDateTime createdAt;
}
