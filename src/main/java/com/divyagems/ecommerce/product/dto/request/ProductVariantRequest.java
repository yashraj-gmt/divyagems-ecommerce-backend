package com.divyagems.ecommerce.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a product variant.
 * A variant represents a color/finish option for a product.
 * Price is optional — if null, the parent product's price is used.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantRequest {

    @Size(max = 100, message = "Color name must not exceed 100 characters")
    private String colorName;

    @Size(max = 10, message = "Color hex code must not exceed 10 characters")
    private String colorHexCode;

    @NotBlank(message = "Variant SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    /** Overrides product price if set. */
    @DecimalMin(value = "0.01", message = "Variant price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Sale price must be greater than 0")
    private BigDecimal salePrice;

    private int stockQuantity = 0;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private boolean isActive = true;
}
