package com.divyagems.ecommerce.wishlist.service.impl;

import com.divyagems.ecommerce.cart.dto.request.AddToCartRequest;
import com.divyagems.ecommerce.cart.dto.response.CartResponse;
import com.divyagems.ecommerce.cart.service.CartService;
import com.divyagems.ecommerce.entity.*;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.product.repository.ProductVariantRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import com.divyagems.ecommerce.wishlist.dto.request.AddToWishlistRequest;
import com.divyagems.ecommerce.wishlist.dto.response.WishlistItemResponse;
import com.divyagems.ecommerce.wishlist.repository.WishlistRepository;
import com.divyagems.ecommerce.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository     wishlistRepository;
    private final ProductRepository      productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository         userRepository;
    private final CartService            cartService;

    // ─── Get My Wishlist ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getMyWishlist() {
        User user = requireCurrentUser();
        return wishlistRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── Add to Wishlist ───────────────────────────────────────

    @Override
    @Transactional
    public void addToWishlist(AddToWishlistRequest request) {
        User user = requireCurrentUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Duplicate guard — silently ignore if already wishlisted
        if (request.getVariantId() == null) {
            if (wishlistRepository.existsByUserAndProductAndVariantIsNull(user, product)) {
                log.debug("Product {} already in wishlist for user {}", product.getId(), user.getEmail());
                return;
            }
        } else {
            if (wishlistRepository.existsByUserAndProduct_IdAndVariant_Id(
                    user, product.getId(), request.getVariantId())) {
                log.debug("Product+variant already in wishlist for user {}", user.getEmail());
                return;
            }
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", request.getVariantId()));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BusinessException("Variant does not belong to the specified product.");
            }
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .variant(variant)
                .build();

        wishlistRepository.save(item);
        log.info("Wishlisted product {} (variant: {}) for user {}", product.getId(), request.getVariantId(), user.getEmail());
    }

    // ─── Remove from Wishlist ──────────────────────────────────

    @Override
    @Transactional
    public void removeFromWishlist(UUID wishlistItemId) {
        User user = requireCurrentUser();
        WishlistItem item = wishlistRepository.findByIdAndUser(wishlistItemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("WishlistItem", "id", wishlistItemId));
        wishlistRepository.delete(item);
        log.info("Removed wishlist item {} for user {}", wishlistItemId, user.getEmail());
    }

    // ─── Is In Wishlist ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(UUID productId) {
        User user = requireCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        return wishlistRepository.existsByUserAndProduct(user, product);
    }

    // ─── Move to Cart ──────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse moveToCart(UUID wishlistItemId, String sessionId) {
        User user = requireCurrentUser();
        WishlistItem item = wishlistRepository.findByIdAndUser(wishlistItemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("WishlistItem", "id", wishlistItemId));

        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(item.getProduct().getId());
        cartRequest.setVariantId(item.getVariant() != null ? item.getVariant().getId() : null);
        cartRequest.setQuantity(1);

        CartResponse cartResponse = cartService.addToCart(cartRequest, sessionId);

        // Remove from wishlist after successful cart add
        wishlistRepository.delete(item);
        log.info("Moved wishlist item {} to cart for user {}", wishlistItemId, user.getEmail());

        return cartResponse;
    }

    // ─── Private — Mapper ──────────────────────────────────────

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        Product product   = item.getProduct();
        ProductVariant v  = item.getVariant();

        // Primary image
        String imageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(product.getImages().isEmpty() ? null
                        : product.getImages().get(0).getImageUrl());

        // Effective prices
        BigDecimal price     = product.getPrice();
        BigDecimal salePrice = product.getSalePrice();

        // Stock: use variant stock if variant is set, else product stock
        int stock = v != null ? v.getStockQuantity() : product.getStockQuantity();

        return WishlistItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .primaryImageUrl(imageUrl)
                .price(price)
                .salePrice(salePrice)
                .variantId(v != null ? v.getId() : null)
                .variantName(v != null ? v.getColorName() : null)
                .variantColorHex(v != null ? v.getColorHexCode() : null)
                .variantImageUrl(v != null && StringUtils.hasText(v.getImageUrl()) ? v.getImageUrl() : null)
                .inStock(stock > 0)
                .addedAt(item.getCreatedAt())
                .build();
    }

    // ─── Private — Auth helper ─────────────────────────────────

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", auth.getName()));
    }
}
