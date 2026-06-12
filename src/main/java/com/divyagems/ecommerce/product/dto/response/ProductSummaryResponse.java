package com.divyagems.ecommerce.product.dto.response;

import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Compact product response for listing cards, search results, and carousels.
 * Contains only fields needed to render a product card in the frontend.
 * discountPercent is pre-calculated: ((price - salePrice) / price) * 100
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String sku;
    private String shortDescription;

    /** URL of the image marked isPrimary=true; falls back to first image. */
    private String primaryImageUrl;

    private BigDecimal price;
    private BigDecimal salePrice;

    /** Percentage discount rounded to 1 decimal. Null if no sale price. */
    private BigDecimal discountPercent;

    private Double averageRating;
    private int totalReviews;
    private int stockQuantity;
    private ProductStatusEnum status;

    /** Active color variants for swatch rendering on listing cards. */
    private List<ColorVariantInfo> colorVariants;

    private boolean isFeatured;
    private boolean isNewArrival;
}
