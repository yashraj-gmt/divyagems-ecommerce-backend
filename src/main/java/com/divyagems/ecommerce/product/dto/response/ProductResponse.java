package com.divyagems.ecommerce.product.dto.response;

import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full product detail response for the product detail page.
 * Extends all ProductSummaryResponse fields with rich content,
 * full tag grouping, all variants, and all images.
 *
 * tagsByType groups tags by their TagTypeEnum name for clean frontend rendering:
 * {
 *   "PLANET":   [{ "id": "...", "name": "Jupiter", "slug": "jupiter" }],
 *   "CHAKRA":   [{ "id": "...", "name": "Crown Chakra", "slug": "crown-chakra" }],
 *   "MATERIAL": [...]
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    // ─── Summary fields ────────────────────────────────────────
    private UUID id;
    private String name;
    private String slug;
    private String sku;
    private String shortDescription;
    private String primaryImageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal discountPercent;
    private Double averageRating;
    private int totalReviews;
    private int stockQuantity;
    private int lowStockThreshold;
    private ProductStatusEnum status;
    private boolean isFeatured;
    private boolean isNewArrival;
    private List<ColorVariantInfo> colorVariants;

    // ─── Rich content ──────────────────────────────────────────
    private String description;
    private String benefits;
    private String usageInstructions;
    private Double weight;
    private String weightUnit;

    // ─── SEO ──────────────────────────────────────────────────
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;

    // ─── Relations ────────────────────────────────────────────
    private CategoryRef category;
    private CategoryRef subCategory;

    /** Tags keyed by their TagTypeEnum name for frontend grouping. */
    private Map<String, List<TagRef>> tagsByType;

    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;

    // ─── Audit ────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Nested reference types ────────────────────────────────

    @Getter
    @Builder
    public static class CategoryRef {
        private UUID id;
        private String name;
        private String slug;
    }

    @Getter
    @Builder
    public static class TagRef {
        private UUID id;
        private String name;
        private String slug;
        private String tagType;
    }
}
