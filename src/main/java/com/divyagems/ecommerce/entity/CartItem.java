package com.divyagems.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Individual line item in a shopping cart.
 * A composite unique constraint prevents duplicate product+variant combos per cart.
 * priceAtAddTime captures the price when added to protect against later price changes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_cart_product_variant",
                        columnNames = {"cart_id", "product_id", "variant_id"}
                )
        }
)
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Null when no variant is selected
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price_at_add_time", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAddTime;
}
