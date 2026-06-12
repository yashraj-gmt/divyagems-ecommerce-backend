package com.divyagems.ecommerce.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a single category.
 * Includes parent info and direct children (one level deep).
 * productCount is the number of products directly assigned to this category.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private int displayOrder;
    private boolean isActive;
    private long productCount;

    /** ID of the parent category — null for top-level categories. */
    private UUID parentId;

    /** Name of the parent category — null for top-level categories. */
    private String parentName;

    /** Direct subcategories (one level deep). Empty list for leaf categories. */
    private List<CategoryResponse> children;
}
