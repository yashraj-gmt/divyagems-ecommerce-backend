package com.divyagems.ecommerce.wishlist.controller;

import com.divyagems.ecommerce.cart.dto.response.CartResponse;
import com.divyagems.ecommerce.wishlist.dto.request.AddToWishlistRequest;
import com.divyagems.ecommerce.wishlist.dto.response.WishlistItemResponse;
import com.divyagems.ecommerce.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Wishlist endpoints — all require a valid JWT (authenticated users only).
 */
@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Save, view, and manage product wishlists")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    // ─── GET /wishlist ────────────────────────────────────────
    @Operation(summary = "Get current user's wishlist")
    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getMyWishlist() {
        return ResponseEntity.ok(wishlistService.getMyWishlist());
    }

    // ─── POST /wishlist ───────────────────────────────────────
    @Operation(summary = "Add a product (with optional variant) to the wishlist")
    @PostMapping
    public ResponseEntity<Void> addToWishlist(@RequestBody AddToWishlistRequest request) {
        wishlistService.addToWishlist(request);
        return ResponseEntity.ok().build();
    }

    // ─── DELETE /wishlist/{id} ────────────────────────────────
    @Operation(summary = "Remove an item from the wishlist")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable UUID id) {
        wishlistService.removeFromWishlist(id);
        return ResponseEntity.noContent().build();
    }

    // ─── GET /wishlist/check?productId=... ────────────────────
    @Operation(summary = "Check if a product is in the current user's wishlist")
    @GetMapping("/check")
    public ResponseEntity<Boolean> isInWishlist(@RequestParam UUID productId) {
        return ResponseEntity.ok(wishlistService.isInWishlist(productId));
    }

    // ─── POST /wishlist/{id}/move-to-cart ─────────────────────
    @Operation(summary = "Move a wishlist item to the active cart")
    @PostMapping("/{id}/move-to-cart")
    public ResponseEntity<CartResponse> moveToCart(
            @PathVariable UUID id,
            @CookieValue(name = "CART_SESSION_ID", required = false) String sessionId) {
        return ResponseEntity.ok(wishlistService.moveToCart(id, sessionId));
    }
}
