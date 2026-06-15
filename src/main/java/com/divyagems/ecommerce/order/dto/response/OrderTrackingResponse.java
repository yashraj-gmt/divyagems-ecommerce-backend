package com.divyagems.ecommerce.order.dto.response;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Public-facing tracking response for GET /api/orders/{orderId}/track.
 * Contains status timeline and shipping info without exposing financial data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingResponse {

    private String orderId;
    private OrderStatusEnum currentStatus;

    /** Full status timeline — oldest entry first. */
    private List<OrderStatusHistoryResponse> statusHistory;

    private String trackingNumber;
    private String shippingProvider;
    private LocalDate estimatedDelivery;
}
