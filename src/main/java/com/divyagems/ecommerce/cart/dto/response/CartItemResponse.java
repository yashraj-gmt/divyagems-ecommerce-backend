package com.divyagems.ecommerce.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/** Compact cart line item response for the shopping cart view. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {

    private UUID cartItemId;

    /** Embedded product snapshot — only the fields needed to render a cart row. */
    private CartItemProductRef product;

    /** Null when no variant is selected. */
    private CartItemVariantRef variant;

    /** Resolved SKU: variant SKU if variant is selected, else product SKU. */
    private String sku;

    private int quantity;

    /** Price captured at time of adding to cart (priceAtAddTime). */
    private BigDecimal unitPrice;

    /** unitPrice × quantity — pre-calculated for the frontend. */
    private BigDecimal totalPrice;

    /** Current available stock for this product/variant at response time. */
    private int stockAvailable;
}
