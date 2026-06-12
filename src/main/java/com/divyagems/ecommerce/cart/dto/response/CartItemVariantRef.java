package com.divyagems.ecommerce.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Embedded variant reference within a CartItemResponse.
 * Null in the parent CartItemResponse when no variant was selected.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemVariantRef {
    private UUID id;
    private String colorName;
    private String colorHexCode;
    private String imageUrl;
}
