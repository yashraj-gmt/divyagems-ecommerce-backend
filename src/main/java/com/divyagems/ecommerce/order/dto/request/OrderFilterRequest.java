package com.divyagems.ecommerce.order.dto.request;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.PaymentMethodEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import lombok.*;

import java.time.LocalDate;

/**
 * Admin filter parameters for GET /api/admin/orders.
 * All fields are optional — null means no filter on that field.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilterRequest {

    /** Filter by order status. */
    private OrderStatusEnum status;

    /** Filter by payment status. */
    private PaymentStatusEnum paymentStatus;

    /** Filter by payment method (ONLINE or COD). */
    private PaymentMethodEnum paymentMethod;

    /** Orders placed on or after this date. */
    private LocalDate fromDate;

    /** Orders placed on or before this date. */
    private LocalDate toDate;

    /**
     * Free-text search — matches against orderId or user email.
     * Applied as a LIKE %search% query.
     */
    private String search;
}
