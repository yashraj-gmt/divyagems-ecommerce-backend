package com.divyagems.ecommerce.product.dto.request;

import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.enums.SortByEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic filter request for product search and listing.
 * All fields are optional — omitted fields are not applied as filters.
 *
 * Tag-based filters (materials, planets, chakras, elements) are resolved
 * to tag IDs in the service layer before being passed to ProductSpecification.
 *
 * Usage: bind via @ModelAttribute on controller methods to support
 * multi-value query params (e.g. ?colorNames=Red&colorNames=Blue).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductFilterRequest {

    /** Full-text search on name, shortDescription, sku. */
    private String keyword;

    private String categorySlug;
    private String subCategorySlug;

    /** Filter by variant color names (OR within this list). */
    private List<String> colorNames;

    /** Filter by specific tag IDs (AND — product must have all). */
    private List<UUID> tagIds;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // ─── Tag-type specific filters (resolved to tagIds in service) ─────────
    /** Tag names of type MATERIAL (e.g. "Rose Quartz", "Amethyst"). */
    private List<String> materials;

    /** Tag names of type PLANET (e.g. "Jupiter", "Venus"). */
    private List<String> planets;

    /** Tag names of type CHAKRA (e.g. "Crown Chakra", "Heart Chakra"). */
    private List<String> chakras;

    /** Tag names of type ELEMENT (e.g. "Earth", "Water"). */
    private List<String> elements;

    // ─── Status & Flag filters ──────────────────────────────────────────────
    private ProductStatusEnum status;
    private Boolean isFeatured;
    private Boolean isNewArrival;

    // ─── Sort & Pagination ─────────────────────────────────────────────────
    private SortByEnum sortBy = SortByEnum.NEWEST;
    private int page = 0;
    private int size = 20;
}
