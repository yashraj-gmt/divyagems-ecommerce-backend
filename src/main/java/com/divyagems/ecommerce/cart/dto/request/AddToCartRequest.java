package com.divyagems.ecommerce.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request to add an item to the cart.
 * variantId is optional — set when the customer selects a colour/finish variant.
 * quantity must be ≥ 1; adding more units of an existing item increments the existing line.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    /** Optional — null means the base product without a specific variant. */
    private UUID variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
