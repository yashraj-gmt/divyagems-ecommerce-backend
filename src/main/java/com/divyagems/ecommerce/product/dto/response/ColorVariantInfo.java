package com.divyagems.ecommerce.product.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight color variant info for product listing cards.
 * Shows available color swatches without loading full variant data.
 */
@Getter
@Builder
public class ColorVariantInfo {
    private String colorName;
    private String colorHexCode;
}
