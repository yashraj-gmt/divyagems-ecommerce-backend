package com.divyagems.ecommerce.cart.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full cart response.
 * subtotal is computed as sum of (unitPrice × quantity) across all line items.
 * totalItems = number of distinct line items (not total quantity).
 * totalQuantity = sum of quantities across all line items.
 */
@Getter
@Builder
public class CartResponse {

    private UUID cartId;
    private List<CartItemResponse> items;

    /** Sum of (unitPrice × quantity) for each item. */
    private BigDecimal subtotal;

    /** Number of distinct product/variant combinations in the cart. */
    private int totalItems;

    /** Sum of all item quantities. */
    private int totalQuantity;
}
