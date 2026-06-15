package com.divyagems.ecommerce.order.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Line-item detail within an OrderResponse.
 * All fields are snapshots captured at order time — immutable history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String productImageUrl;
}
