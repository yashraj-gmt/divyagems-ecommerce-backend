package com.divyagems.ecommerce.cart.service;

import com.divyagems.ecommerce.cart.dto.request.AddToCartRequest;
import com.divyagems.ecommerce.cart.dto.request.UpdateCartItemRequest;
import com.divyagems.ecommerce.cart.dto.response.CartResponse;
import com.divyagems.ecommerce.cart.dto.response.CartValidationResponse;

import java.util.UUID;

public interface CartService {

    /**
     * Retrieve the active cart for the current principal.
     * - Authenticated user: find by userId, create if none.
     * - Guest: find by sessionId, create if none.
     *
     * @param sessionId guest session cookie value; ignored when user is authenticated
     * @return the current cart
     */
    CartResponse getOrCreateCart(String sessionId);

    /**
     * Add an item to the cart.
     * If the same product+variant already exists, quantity is incremented.
     * Captures effective price (salePrice if set, else price) as priceAtAddTime.
     *
     * @param request   the item to add
     * @param sessionId guest session ID (ignored if user is authenticated)
     */
    CartResponse addToCart(AddToCartRequest request, String sessionId);

    /**
     * Update the quantity of an existing cart item.
     * quantity = 0 removes the item (equivalent to removeCartItem).
     *
     * @param cartItemId the UUID of the CartItem to update
     * @param request    contains the new quantity
     * @param sessionId  guest session ID (for authorization scoping)
     */
    CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request, String sessionId);

    /**
     * Remove a single item from the cart.
     *
     * @param cartItemId the UUID of the CartItem to remove
     * @param sessionId  guest session ID
     */
    CartResponse removeCartItem(UUID cartItemId, String sessionId);

    /**
     * Remove all items from the current cart (keeps the cart entity active).
     *
     * @param sessionId guest session ID
     */
    void clearCart(String sessionId);

    /**
     * Merge a guest cart into the authenticated user's active cart.
     * Called automatically after login.
     *
     * Strategy:
     * - Same product+variant already in user cart → keep whichever quantity is higher
     * - Items only in guest cart → moved to user cart
     * - Guest cart is deactivated after merge
     *
     * @param sessionId the guest session ID whose cart will be merged
     */
    void mergeGuestCart(String sessionId);

    /**
     * Pre-checkout validation.
     * Checks each item for:
     * - Product/variant still active
     * - Sufficient stock
     * - Price changes since item was added
     *
     * Returns a CartValidationResponse with a valid flag and detailed issue list.
     * The subtotal is recalculated at current prices.
     *
     * @param sessionId guest session ID
     */
    CartValidationResponse validateCart(String sessionId);
}
