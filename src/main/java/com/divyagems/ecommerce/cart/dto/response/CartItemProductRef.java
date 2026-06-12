package com.divyagems.ecommerce.cart.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Embedded product reference within a CartItemResponse.
 * Only the fields needed to render a cart row without hitting the product API.
 */
@Getter
@Builder
public class CartItemProductRef {
    private UUID id;
    private String name;
    private String slug;
    private String primaryImageUrl;
    private String status;
}
