package com.divyagems.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User wishlist item — a saved product (optionally with a specific variant).
 * Composite unique constraint prevents duplicate saves of the same product+variant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "wishlist_items",
        indexes = {
                @Index(name = "idx_wishlist_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_wishlist_user_product_variant",
                        columnNames = {"user_id", "product_id", "variant_id"}
                )
        }
)
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Null means wishlisted without a specific variant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;
}
