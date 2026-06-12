package com.divyagems.ecommerce.product.dto.request;

import com.divyagems.ecommerce.enums.ProductStatusEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating or updating a Product.
 * Slug is auto-generated from name if left blank.
 * Images and variants are managed inline (cascade save).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 300, message = "Product name must not exceed 300 characters")
    private String name;

    /** Auto-generated from name if blank. */
    @Size(max = 350, message = "Slug must not exceed 350 characters")
    private String slug;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private UUID subCategoryId;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    private String description;
    private String benefits;
    private String usageInstructions;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Sale price must be greater than 0")
    private BigDecimal salePrice;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQuantity = 0;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private int lowStockThreshold = 5;

    private Double weight;

    @Size(max = 20)
    private String weightUnit = "grams";

    private ProductStatusEnum status = ProductStatusEnum.DRAFT;

    private boolean isFeatured = false;
    private boolean isNewArrival = false;

    @Size(max = 200, message = "Meta title must not exceed 200 characters")
    private String metaTitle;

    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    private String metaDescription;

    @Size(max = 300, message = "Meta keywords must not exceed 300 characters")
    private String metaKeywords;

    /** IDs of Tag entities to associate with this product. */
    private List<UUID> tagIds = new ArrayList<>();

    @Valid
    private List<ProductVariantRequest> variants = new ArrayList<>();

    @Valid
    private List<ProductImageRequest> images = new ArrayList<>();
}
