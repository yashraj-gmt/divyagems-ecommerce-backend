package com.divyagems.ecommerce.cart.repository;

import com.divyagems.ecommerce.entity.Cart;
import com.divyagems.ecommerce.entity.CartItem;
import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Find a cart item by ID scoped to the given cart.
     * Prevents cross-cart item access.
     */
    Optional<CartItem> findByIdAndCart(UUID id, Cart cart);

    /**
     * Find an existing line item for a specific product + variant combo.
     * Used to detect duplicates on addToCart → increment instead of insert.
     */
    Optional<CartItem> findByCartAndProductAndVariant(
            Cart cart, Product product, ProductVariant variant);

    /**
     * Find an existing line item for a product with NO variant selected.
     * Separate method because JPQL cannot match NULL via equality.
     */
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart = :cart AND ci.product = :product AND ci.variant IS NULL")
    Optional<CartItem> findByCartAndProductWithNoVariant(
            @Param("cart") Cart cart,
            @Param("product") Product product);

    /**
     * All items in a cart, ordered by creation date for a stable display order.
     */
    List<CartItem> findByCartOrderByCreatedAtAsc(Cart cart);

    /** Count distinct items (line items) in a cart. */
    int countByCart(Cart cart);

    /** Delete all items in a cart (used on clearCart). */
    void deleteAllByCart(Cart cart);
}
