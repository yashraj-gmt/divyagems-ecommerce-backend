package com.divyagems.ecommerce.cart.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Result of pre-checkout cart validation.
 *
 * valid = true means all items are available at the captured prices and
 * the cart is safe to proceed to checkout.
 *
 * When valid = false, the frontend should display the issues and prompt the
 * customer to review their cart before proceeding.
 *
 * subtotal is recalculated at current prices so the frontend can show
 * an updated total even before the customer manually refreshes their cart.
 */
@Getter
@Builder
public class CartValidationResponse {

    /** True only when issues is empty. */
    private boolean valid;

    /** Recalculated subtotal using current effective prices (not priceAtAddTime). */
    private BigDecimal subtotal;

    private List<CartItemIssue> issues;

    // ─── Nested types ──────────────────────────────────────────

    @Getter
    @Builder
    public static class CartItemIssue {
        private UUID cartItemId;
        private String productName;
        private String variantInfo;    // e.g. "Red / #FF0000" — null if no variant
        private IssueType issueType;
        private String message;

        /** Set for INSUFFICIENT_STOCK — how many are actually available. */
        private Integer availableStock;

        /** Set for PRICE_CHANGED — the price currently shown on the product page. */
        private BigDecimal currentPrice;

        /** Set for PRICE_CHANGED — the price stored in the cart item. */
        private BigDecimal cartPrice;
    }

    public enum IssueType {
        /** Product has been set to INACTIVE or DRAFT — cannot be purchased. */
        PRODUCT_INACTIVE,
        /** Selected variant has been deactivated. */
        VARIANT_INACTIVE,
        /** Stock is completely exhausted. */
        OUT_OF_STOCK,
        /** Stock is lower than requested quantity. */
        INSUFFICIENT_STOCK,
        /**
         * The effective price has changed since the item was added.
         * The cart price is honoured during the current session but updated on
         * next add-to-cart for the same product.
         */
        PRICE_CHANGED
    }
}
