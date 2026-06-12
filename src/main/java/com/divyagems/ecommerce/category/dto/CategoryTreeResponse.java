package com.divyagems.ecommerce.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Recursive tree response for the category hierarchy.
 * Top-level categories contain nested subcategories in 'children'.
 * Designed for frontend navigation menus.
 *
 * Example:
 * {
 *   "id": "...",
 *   "name": "Gemstones",
 *   "slug": "gemstones",
 *   "children": [
 *     { "id": "...", "name": "Ruby", "slug": "ruby", "children": [] },
 *     { "id": "...", "name": "Emerald", "slug": "emerald", "children": [] }
 *   ]
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryTreeResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private int displayOrder;
    private boolean isActive;
    private long productCount;

    /** Recursively nested subcategories. */
    private List<CategoryTreeResponse> children;
}
