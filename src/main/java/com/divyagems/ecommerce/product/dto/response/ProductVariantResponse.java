package com.divyagems.ecommerce.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full variant response including variant-level images.
 * Effective price = variant.price if set, otherwise product.price.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse {
    private UUID id;
    private String colorName;
    private String colorHexCode;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private int stockQuantity;
    private String imageUrl;
    private boolean isActive;
    private List<ProductImageResponse> variantImages;
}
