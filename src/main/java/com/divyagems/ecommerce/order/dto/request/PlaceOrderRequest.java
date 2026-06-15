package com.divyagems.ecommerce.order.dto.request;

import com.divyagems.ecommerce.enums.PaymentMethodEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * Request body for POST /api/orders.
 *
 * Address resolution rules:
 *  - shippingAddressId → use saved address (UUID reference)
 *  - newShippingAddress → use inline address (validated inline)
 *  - Exactly one of the two must be provided (enforced in service layer).
 *
 *  - sameAsBilling=true → billing address is copied from shipping
 *  - Otherwise: billingAddressId OR newBillingAddress must be provided.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    // ─── Shipping Address ─────────────────────────────────
    /** UUID of a saved address in the user's address book. */
    private UUID shippingAddressId;

    /** Inline new address — used when shippingAddressId is null. */
    @Valid
    private AddressRequest newShippingAddress;

    // ─── Billing Address ──────────────────────────────────
    /** If true, billing address is identical to shipping — ignore billing fields. */
    @Builder.Default
    private boolean sameAsBilling = false;

    /** UUID of a saved billing address. Ignored if sameAsBilling=true. */
    private UUID billingAddressId;

    /** Inline new billing address. Ignored if sameAsBilling=true. */
    @Valid
    private AddressRequest newBillingAddress;

    // ─── Coupon ───────────────────────────────────────────
    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String couponCode;

    // ─── Payment ──────────────────────────────────────────
    @NotNull(message = "Payment method is required")
    private PaymentMethodEnum paymentMethod;

    // ─── Notes ───────────────────────────────────────────
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
