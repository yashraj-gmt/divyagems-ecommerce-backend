package com.divyagems.ecommerce.wishlist.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToWishlistRequest {

    /** The product to wishlist. */
    private UUID productId;

    /**
     * Optional — wishlist a specific variant (e.g. a particular gemstone colour).
     * Null means the product without a variant preference.
     */
    private UUID variantId;
}
