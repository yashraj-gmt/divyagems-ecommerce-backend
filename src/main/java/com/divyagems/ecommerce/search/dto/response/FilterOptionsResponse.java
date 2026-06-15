package com.divyagems.ecommerce.search.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Available filter options for the product catalogue.
 * Returned by GET /search/filters so the frontend can render dynamic filter panels.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterOptionsResponse {

    // Tag-based filters — list of names for each spiritual attribute
    private List<String> materials;
    private List<String> planets;
    private List<String> chakras;
    private List<String> elements;
    private List<String> benefits;
    private List<String> vastuUses;

    // Active variant color names + hex codes
    private List<ColorOption> colors;

    // Absolute price range across all active products
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorOption {
        private String name;
        private String hexCode;
    }
}
