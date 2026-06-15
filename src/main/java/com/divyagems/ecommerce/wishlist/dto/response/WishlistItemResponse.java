package com.divyagems.ecommerce.wishlist.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wishlist item response including a product summary and real-time stock status.
 * inStock is derived at query time from current stockQuantity / variant stockQuantity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {

    private UUID id;

    // ─── Product summary ──────────────────────────────────────
    private UUID productId;
    private String productName;
    private String productSlug;
    private String primaryImageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;

    // ─── Variant info (null when no specific variant) ─────────
    private UUID variantId;
    private String variantName;
    private String variantColorHex;
    private String variantImageUrl;

    /** True when the selected variant (or base product) has stock > 0. */
    private boolean inStock;

    private LocalDateTime addedAt;
}
