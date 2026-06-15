package com.divyagems.ecommerce.search.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Lightweight autocomplete suggestion item. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSuggestion {

    private UUID   productId;
    private String name;
    private String slug;
    private String primaryImageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;
}
