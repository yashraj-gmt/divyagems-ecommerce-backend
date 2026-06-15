package com.divyagems.ecommerce.wishlist.service;

import com.divyagems.ecommerce.cart.dto.response.CartResponse;
import com.divyagems.ecommerce.wishlist.dto.request.AddToWishlistRequest;
import com.divyagems.ecommerce.wishlist.dto.response.WishlistItemResponse;

import java.util.List;
import java.util.UUID;

public interface WishlistService {

    /** Return all wishlisted items for the current user, newest first. */
    List<WishlistItemResponse> getMyWishlist();

    /**
     * Add a product (with optional variant) to the current user's wishlist.
     * Silently ignores duplicates — does not throw if already wishlisted.
     */
    void addToWishlist(AddToWishlistRequest request);

    /**
     * Remove a wishlist item by its own ID.
     * Enforces ownership — throws 404 if the item doesn't belong to the current user.
     */
    void removeFromWishlist(UUID wishlistItemId);

    /**
     * Check whether the current user has any wishlist entry for the given product
     * (regardless of variant).
     */
    boolean isInWishlist(UUID productId);

    /**
     * Move a wishlist item to the active cart, then remove it from the wishlist.
     * Uses quantity=1 and the variant from the wishlist item.
     * Respects all cart stock/status validations.
     *
     * @param wishlistItemId the wishlist item to move
     * @param sessionId      guest session cookie (forwarded to CartService)
     * @return the updated cart
     */
    CartResponse moveToCart(UUID wishlistItemId, String sessionId);
}
