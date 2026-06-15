package com.divyagems.ecommerce.order.dto.request;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Admin request to move an order to a new status.
 * trackingNumber and shippingProvider are optional — set when transitioning to SHIPPED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatusEnum status;

    /** Optional admin comment attached to the status history entry. */
    @Size(max = 500)
    private String comment;

    /** Courier tracking number — provided when shipping. */
    @Size(max = 100)
    private String trackingNumber;

    /** Shipping carrier name (e.g. "BlueDart", "Delhivery"). */
    @Size(max = 100)
    private String shippingProvider;
}
