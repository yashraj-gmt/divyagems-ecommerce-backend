package com.divyagems.ecommerce.cart.controller;

import com.divyagems.ecommerce.cart.dto.request.AddToCartRequest;
import com.divyagems.ecommerce.cart.dto.request.UpdateCartItemRequest;
import com.divyagems.ecommerce.cart.dto.response.CartResponse;
import com.divyagems.ecommerce.cart.dto.response.CartValidationResponse;
import com.divyagems.ecommerce.cart.service.CartService;
import com.divyagems.ecommerce.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Cart REST controller.
 *
 * Session Management:
 * The cart supports both authenticated users and guests.
 * Guests are identified by a {@code CART_SESSION_ID} cookie set by this controller.
 * On first request without a cookie, a new UUID session ID is generated and
 * returned in a Set-Cookie header. All subsequent guest requests must include
 * this cookie for cart operations.
 *
 * Authenticated users' carts are stored against their user ID. The session cookie
 * is ignored for authenticated requests (the service uses SecurityContext instead).
 *
 * Security Config note:
 * Add "/cart/**" to the list of paths that do NOT require authentication in SecurityConfig
 * (except /cart/merge which requires authentication).
 * SecurityConfig should include:
 *   .requestMatchers("/cart/**").permitAll()
 *   .requestMatchers("/cart/merge").authenticated()
 *
 * Full URLs (context-path = /api/v1):
 *   GET    /api/v1/cart
 *   POST   /api/v1/cart/items
 *   PUT    /api/v1/cart/items/{id}
 *   DELETE /api/v1/cart/items/{id}
 *   DELETE /api/v1/cart
 *   POST   /api/v1/cart/validate
 *   POST   /api/v1/cart/merge   [AUTHENTICATED]
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations — supports both authenticated users and guests")
public class CartController {

    private static final String SESSION_COOKIE_NAME = "CART_SESSION_ID";
    private static final int    SESSION_COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30 days

    private final CartService cartService;

    // ──────────────────────────────────────────────────────────
    // GET /cart — Retrieve or create the current cart
    // ──────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "Get current cart",
            description = "Returns the active cart for the authenticated user or guest session. " +
                          "A new cart is created if none exists. " +
                          "Guest carts are identified by the CART_SESSION_ID cookie."
    )
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(
            HttpServletRequest request,
            HttpServletResponse response) {

        String sessionId = resolveSessionId(request, response);
        CartResponse cart = cartService.getOrCreateCart(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cart));
    }

    // ──────────────────────────────────────────────────────────
    // POST /cart/items — Add an item
    // ──────────────────────────────────────────────────────────

    @PostMapping("/items")
    @Operation(
            summary = "Add item to cart",
            description = "Adds a product (with optional variant) to the cart. " +
                          "If the same product+variant already exists, the quantity is incremented. " +
                          "Price is captured at the time of adding."
    )
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest addRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sessionId = resolveSessionId(request, response);
        CartResponse cart = cartService.addToCart(addRequest, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    // ──────────────────────────────────────────────────────────
    // PUT /cart/items/{id} — Update quantity
    // ──────────────────────────────────────────────────────────

    @PutMapping("/items/{id}")
    @Operation(
            summary = "Update cart item quantity",
            description = "Sets the quantity for an existing cart item. " +
                          "quantity=0 removes the item (same as DELETE /cart/items/{id})."
    )
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCartItemRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sessionId = resolveSessionId(request, response);
        CartResponse cart = cartService.updateCartItem(id, updateRequest, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", cart));
    }

    // ──────────────────────────────────────────────────────────
    // DELETE /cart/items/{id} — Remove a single item
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/items/{id}")
    @Operation(
            summary = "Remove item from cart",
            description = "Removes a single line item from the cart by its cart item ID."
    )
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable UUID id,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sessionId = resolveSessionId(request, response);
        CartResponse cart = cartService.removeCartItem(id, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    // ──────────────────────────────────────────────────────────
    // DELETE /cart — Clear all items
    // ──────────────────────────────────────────────────────────

    @DeleteMapping
    @Operation(
            summary = "Clear cart",
            description = "Removes all items from the cart. The cart entity itself is preserved."
    )
    public ResponseEntity<ApiResponse<Void>> clearCart(
            HttpServletRequest request,
            HttpServletResponse response) {

        String sessionId = resolveSessionId(request, response);
        cartService.clearCart(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
    }

    // ──────────────────────────────────────────────────────────
    // POST /cart/validate — Pre-checkout validation
    // ──────────────────────────────────────────────────────────

    @PostMapping("/validate")
    @Operation(
            summary = "Validate cart before checkout",
            description = "Checks all cart items for stock availability, product status, " +
                          "and price changes. Returns a validation result with any issues found. " +
                          "Call this before creating an order."
    )
    public ResponseEntity<ApiResponse<CartValidationResponse>> validateCart(
            HttpServletRequest request,
            HttpServletResponse response) {

        String sessionId = resolveSessionId(request, response);
        CartValidationResponse validation = cartService.validateCart(sessionId);

        String msg = validation.isValid()
                ? "Cart is valid and ready for checkout"
                : "Cart has issues that must be resolved before checkout";

        return ResponseEntity.ok(ApiResponse.success(msg, validation));
    }

    // ──────────────────────────────────────────────────────────
    // POST /cart/merge — Merge guest cart into user cart [AUTH]
    // ──────────────────────────────────────────────────────────

    @PostMapping("/merge")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Merge guest cart into user cart [AUTHENTICATED]",
            description = "Merges the guest cart (identified by CART_SESSION_ID cookie) into " +
                          "the authenticated user's cart. Call this immediately after login. " +
                          "Guest cart is deactivated after merge. " +
                          "For duplicate items, the higher quantity is kept (capped by stock)."
    )
    public ResponseEntity<ApiResponse<Void>> mergeGuestCart(
            @Parameter(description = "The guest session ID to merge from")
            @RequestParam(required = false) String sessionId,
            HttpServletRequest request) {

        // If sessionId not provided as param, try to read from cookie
        String resolvedSessionId = sessionId != null ? sessionId : readSessionCookie(request);
        cartService.mergeGuestCart(resolvedSessionId);
        return ResponseEntity.ok(ApiResponse.success("Guest cart merged successfully"));
    }

    // ─── Private: Session Cookie Management ───────────────────

    /**
     * Read the existing CART_SESSION_ID cookie, or generate and set a new one
     * if none exists. This ensures guests always have a stable session identifier
     * across requests without requiring authentication.
     *
     * @return the session ID (existing or freshly generated)
     */
    private String resolveSessionId(HttpServletRequest request, HttpServletResponse response) {
        String existing = readSessionCookie(request);
        if (existing != null) {
            return existing;
        }
        // Generate a new session ID for this guest
        String newSessionId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, newSessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(SESSION_COOKIE_MAX_AGE);
        // Set Secure in production; use spring.profiles to toggle
        // cookie.setSecure(true);
        response.addCookie(cookie);
        return newSessionId;
    }

    private String readSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> SESSION_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
