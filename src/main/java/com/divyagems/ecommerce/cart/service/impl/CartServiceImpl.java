package com.divyagems.ecommerce.cart.service.impl;

import com.divyagems.ecommerce.cart.dto.request.AddToCartRequest;
import com.divyagems.ecommerce.cart.dto.request.UpdateCartItemRequest;
import com.divyagems.ecommerce.cart.dto.response.*;
import com.divyagems.ecommerce.cart.repository.CartItemRepository;
import com.divyagems.ecommerce.cart.repository.CartRepository;
import com.divyagems.ecommerce.cart.service.CartService;
import com.divyagems.ecommerce.entity.*;
import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.product.repository.ProductVariantRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository         cartRepository;
    private final CartItemRepository     cartItemRepository;
    private final ProductRepository      productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository         userRepository;

    // ─── Get or Create Cart ────────────────────────────────────

    @Override
    @Transactional
    public CartResponse getOrCreateCart(String sessionId) {
        Cart cart = resolveCart(sessionId);
        return buildCartResponse(cart);
    }

    // ─── Add to Cart ───────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request, String sessionId) {
        Cart cart = resolveCart(sessionId);

        // 1. Validate product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (product.getStatus() != ProductStatusEnum.ACTIVE) {
            throw new BusinessException(
                    "Product '" + product.getName() + "' is not available for purchase");
        }

        // 2. Validate variant (if provided)
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product variant", "id", request.getVariantId()));
            if (!variant.isActive()) {
                throw new BusinessException("The selected variant is no longer available");
            }
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BusinessException("Variant does not belong to the specified product");
            }
        }

        // 3. Check stock
        int availableStock = resolveStock(product, variant);
        if (availableStock <= 0) {
            throw new BusinessException("'" + product.getName() + "' is out of stock");
        }

        // 4. Determine price at add time: variant.price → product.salePrice → product.price
        BigDecimal capturedPrice = resolveEffectivePrice(product, variant);

        // 5. Find or create cart item
        final ProductVariant finalVariant = variant;
        Optional<CartItem> existingItem = findExistingCartItem(cart, product, variant);

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            if (newQuantity > availableStock) {
                throw new BusinessException(
                        "Only " + availableStock + " unit(s) available for '" + product.getName() + "'");
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setPriceAtAddTime(capturedPrice);  // refresh price on re-add
        } else {
            if (request.getQuantity() > availableStock) {
                throw new BusinessException(
                        "Only " + availableStock + " unit(s) available for '" + product.getName() + "'");
            }
            cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(finalVariant)
                    .quantity(request.getQuantity())
                    .priceAtAddTime(capturedPrice)
                    .build();
            cart.getCartItems().add(cartItem);
        }

        cartRepository.save(cart);
        log.info("Added to cart: product='{}' qty={} cartId={}", product.getName(),
                request.getQuantity(), cart.getId());

        return buildCartResponse(cart);
    }

    // ─── Update Cart Item ──────────────────────────────────────

    @Override
    @Transactional
    public CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request, String sessionId) {
        Cart cart = resolveCart(sessionId);

        CartItem cartItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        if (request.getQuantity() == 0) {
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Removed cart item: {} (quantity set to 0)", cartItemId);
        } else {
            // Re-check stock against new quantity
            int availableStock = resolveStock(cartItem.getProduct(), cartItem.getVariant());
            if (request.getQuantity() > availableStock) {
                throw new BusinessException(
                        "Only " + availableStock + " unit(s) available for '"
                        + cartItem.getProduct().getName() + "'");
            }
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
        }

        return buildCartResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    // ─── Remove Cart Item ──────────────────────────────────────

    @Override
    @Transactional
    public CartResponse removeCartItem(UUID cartItemId, String sessionId) {
        Cart cart = resolveCart(sessionId);

        CartItem cartItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        log.info("Removed cart item: {} from cart: {}", cartItemId, cart.getId());

        return buildCartResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    // ─── Clear Cart ────────────────────────────────────────────

    @Override
    @Transactional
    public void clearCart(String sessionId) {
        Cart cart = resolveCart(sessionId);
        cartItemRepository.deleteAllByCart(cart);
        cart.getCartItems().clear();
        cartRepository.save(cart);
        log.info("Cart cleared: {}", cart.getId());
    }

    // ─── Merge Guest Cart ──────────────────────────────────────

    @Override
    @Transactional
    public void mergeGuestCart(String sessionId) {
        if (!StringUtils.hasText(sessionId)) return;

        // Authenticated user must be present
        User user = getCurrentUser()
                .orElseThrow(() -> new BusinessException("Must be logged in to merge cart"));

        Optional<Cart> guestCartOpt = cartRepository.findBySessionIdAndIsActiveTrue(sessionId);
        if (guestCartOpt.isEmpty() || guestCartOpt.get().getCartItems().isEmpty()) {
            log.debug("No guest cart to merge for session: {}", sessionId);
            return;
        }
        Cart guestCart = guestCartOpt.get();

        Cart userCart = cartRepository.findByUserAndIsActiveTrue(user)
                .orElseGet(() -> createNewUserCart(user));

        // Merge items: guest items → user cart
        for (CartItem guestItem : guestCart.getCartItems()) {
            Optional<CartItem> existing = findExistingCartItem(
                    userCart, guestItem.getProduct(), guestItem.getVariant());

            if (existing.isPresent()) {
                // Keep the higher quantity — guest is usually the more recent session
                CartItem userItem = existing.get();
                int merged = Math.max(userItem.getQuantity(), guestItem.getQuantity());
                int stock   = resolveStock(guestItem.getProduct(), guestItem.getVariant());
                userItem.setQuantity(Math.min(merged, stock));
                cartItemRepository.save(userItem);
            } else {
                // Move item to user cart
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .product(guestItem.getProduct())
                        .variant(guestItem.getVariant())
                        .quantity(guestItem.getQuantity())
                        .priceAtAddTime(guestItem.getPriceAtAddTime())
                        .build();
                userCart.getCartItems().add(newItem);
            }
        }

        cartRepository.save(userCart);

        // Deactivate guest cart
        guestCart.setActive(false);
        cartRepository.save(guestCart);
        log.info("Guest cart merged into user cart. guestSession={} userId={}", sessionId, user.getId());
    }

    // ─── Validate Cart ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CartValidationResponse validateCart(String sessionId) {
        Cart cart = resolveCart(sessionId);
        List<CartItem> items = cartItemRepository.findByCartOrderByCreatedAtAsc(cart);

        List<CartValidationResponse.CartItemIssue> issues = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product  product = item.getProduct();
            ProductVariant variant = item.getVariant();
            List<CartValidationResponse.CartItemIssue> itemIssues = new ArrayList<>();

            // ── Product status check ──────────────────────────────
            if (product.getStatus() != ProductStatusEnum.ACTIVE) {
                itemIssues.add(buildIssue(item,
                        CartValidationResponse.IssueType.PRODUCT_INACTIVE,
                        "'" + product.getName() + "' is no longer available",
                        null, null, null));
            }

            // ── Variant active check ───────────────────────────────
            if (variant != null && !variant.isActive()) {
                itemIssues.add(buildIssue(item,
                        CartValidationResponse.IssueType.VARIANT_INACTIVE,
                        "The selected variant for '" + product.getName() + "' is no longer available",
                        null, null, null));
            }

            // ── Stock check ────────────────────────────────────────
            int stock = resolveStock(product, variant);
            if (stock <= 0) {
                itemIssues.add(buildIssue(item,
                        CartValidationResponse.IssueType.OUT_OF_STOCK,
                        "'" + product.getName() + "' is out of stock",
                        0, null, null));
            } else if (item.getQuantity() > stock) {
                itemIssues.add(buildIssue(item,
                        CartValidationResponse.IssueType.INSUFFICIENT_STOCK,
                        "Only " + stock + " unit(s) of '" + product.getName() + "' available",
                        stock, null, null));
            }

            // ── Price change check ─────────────────────────────────
            BigDecimal currentPrice = resolveEffectivePrice(product, variant);
            if (item.getPriceAtAddTime().compareTo(currentPrice) != 0) {
                itemIssues.add(buildIssue(item,
                        CartValidationResponse.IssueType.PRICE_CHANGED,
                        "Price for '" + product.getName() + "' has changed",
                        null, currentPrice, item.getPriceAtAddTime()));
            }

            issues.addAll(itemIssues);

            // Accumulate subtotal at current price (regardless of issues)
            subtotal = subtotal.add(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        return CartValidationResponse.builder()
                .valid(issues.isEmpty())
                .subtotal(subtotal)
                .issues(issues)
                .build();
    }

    // ─── Private: Cart Resolution ──────────────────────────────

    /**
     * Determine the active cart for the current request context.
     * Authenticated user → user cart; anonymous → session cart.
     */
    private Cart resolveCart(String sessionId) {
        Optional<User> userOpt = getCurrentUser();
        if (userOpt.isPresent()) {
            return cartRepository.findByUserAndIsActiveTrue(userOpt.get())
                    .orElseGet(() -> createNewUserCart(userOpt.get()));
        } else {
            if (!StringUtils.hasText(sessionId)) {
                throw new BusinessException(
                        "A session ID is required for guest cart access. " +
                        "Ensure the CART_SESSION_ID cookie is sent with the request.");
            }
            return cartRepository.findBySessionIdAndIsActiveTrue(sessionId)
                    .orElseGet(() -> createNewGuestCart(sessionId));
        }
    }

    private Cart createNewUserCart(User user) {
        Cart cart = Cart.builder().user(user).isActive(true).build();
        return cartRepository.save(cart);
    }

    private Cart createNewGuestCart(String sessionId) {
        Cart cart = Cart.builder().sessionId(sessionId).isActive(true).build();
        return cartRepository.save(cart);
    }

    // ─── Private: Response Builders ───────────────────────────

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .sorted(Comparator.comparing(BaseEntity::getCreatedAt))
                .map(this::buildCartItemResponse)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = items.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .build();
    }

    private CartItemResponse buildCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        // Resolve primary image URL
        String primaryImageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(product.getImages().isEmpty() ? null
                        : product.getImages().get(0).getImageUrl());

        CartItemProductRef productRef = CartItemProductRef.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .primaryImageUrl(primaryImageUrl)
                .status(product.getStatus().name())
                .build();

        CartItemVariantRef variantRef = variant == null ? null : CartItemVariantRef.builder()
                .id(variant.getId())
                .colorName(variant.getColorName())
                .colorHexCode(variant.getColorHexCode())
                .imageUrl(variant.getImageUrl())
                .build();

        String sku = variant != null ? variant.getSku() : product.getSku();
        BigDecimal unitPrice = item.getPriceAtAddTime();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .product(productRef)
                .variant(variantRef)
                .sku(sku)
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .stockAvailable(resolveStock(product, variant))
                .build();
    }

    // ─── Private: Price & Stock Helpers ───────────────────────

    /**
     * Effective selling price for a product (optionally variant-scoped).
     * Priority: variant.price → product.salePrice → product.price
     */
    private BigDecimal resolveEffectivePrice(Product product, ProductVariant variant) {
        if (variant != null && variant.getPrice() != null
                && variant.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getSalePrice() != null
                    && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                    ? variant.getSalePrice()
                    : variant.getPrice();
        }
        return product.getSalePrice() != null
                && product.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                ? product.getSalePrice()
                : product.getPrice();
    }

    /** Available stock: variant.stockQuantity if variant, else product.stockQuantity. */
    private int resolveStock(Product product, ProductVariant variant) {
        return variant != null ? variant.getStockQuantity() : product.getStockQuantity();
    }

    /** Find an existing CartItem for the given product+variant combo (null-safe). */
    private Optional<CartItem> findExistingCartItem(Cart cart, Product product, ProductVariant variant) {
        return variant == null
                ? cartItemRepository.findByCartAndProductWithNoVariant(cart, product)
                : cartItemRepository.findByCartAndProductAndVariant(cart, product, variant);
    }

    // ─── Private: Auth Helpers ─────────────────────────────────

    /**
     * Return the currently authenticated User, if any.
     * Returns empty Optional for anonymous/guest requests.
     */
    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByEmail(auth.getName());
    }

    // ─── Private: Validation Issue Builder ────────────────────

    private CartValidationResponse.CartItemIssue buildIssue(
            CartItem item,
            CartValidationResponse.IssueType type,
            String message,
            Integer availableStock,
            BigDecimal currentPrice,
            BigDecimal cartPrice) {

        ProductVariant variant = item.getVariant();
        String variantInfo = variant == null ? null
                : (variant.getColorName() != null ? variant.getColorName() : "")
                  + (variant.getColorHexCode() != null ? " / " + variant.getColorHexCode() : "");

        return CartValidationResponse.CartItemIssue.builder()
                .cartItemId(item.getId())
                .productName(item.getProduct().getName())
                .variantInfo(variantInfo)
                .issueType(type)
                .message(message)
                .availableStock(availableStock)
                .currentPrice(currentPrice)
                .cartPrice(cartPrice)
                .build();
    }
}
