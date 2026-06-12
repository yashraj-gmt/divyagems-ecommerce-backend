package com.divyagems.ecommerce.cart.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to update the quantity of an existing cart item.
 * quantity = 0 is a shortcut for removing the item entirely.
 * This allows the frontend to use a single unified "quantity change" API call.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateCartItemRequest {

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
}
