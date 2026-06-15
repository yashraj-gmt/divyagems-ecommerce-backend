package com.divyagems.ecommerce.order.dto.response;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single entry in the order's status audit trail.
 * Used in both OrderResponse.statusHistory and OrderTrackingResponse.statusHistory.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {

    private OrderStatusEnum status;
    private String comment;
    private String changedBy;
    private LocalDateTime changedAt;
}
