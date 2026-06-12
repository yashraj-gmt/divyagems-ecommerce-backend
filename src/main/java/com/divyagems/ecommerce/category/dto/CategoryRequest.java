package com.divyagems.ecommerce.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload for creating or updating a Category.
 * Slug is auto-generated from name in the service layer if left blank.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Category name must not exceed 150 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    /**
     * Optional. If omitted, slug is auto-generated from name.
     * Must be unique and URL-safe (lowercase, hyphens only).
     */
    @Size(max = 200, message = "Slug must not exceed 200 characters")
    private String slug;

    /** UUID of the parent category. Null = top-level category. */
    private UUID parentId;

    private int displayOrder = 0;

    private boolean isActive = true;
}
