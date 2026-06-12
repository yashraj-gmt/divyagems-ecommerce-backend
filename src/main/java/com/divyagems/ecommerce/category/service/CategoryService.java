package com.divyagems.ecommerce.category.service;

import com.divyagems.ecommerce.category.dto.CategoryRequest;
import com.divyagems.ecommerce.category.dto.CategoryResponse;
import com.divyagems.ecommerce.category.dto.CategoryTreeResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    /**
     * Create a new category. Slug is auto-generated from name if not provided.
     * [ADMIN ONLY]
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Update an existing category by its UUID.
     * Slug is regenerated if the name changes and no explicit slug is provided.
     * [ADMIN ONLY]
     */
    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    /**
     * Delete a category by its UUID.
     * Throws BusinessException if the category has associated products or subcategories.
     * [ADMIN ONLY]
     */
    void deleteCategory(UUID id);

    /**
     * Fetch a single category by its UUID. Throws ResourceNotFoundException if missing.
     */
    CategoryResponse getCategoryById(UUID id);

    /**
     * Fetch a single category by its URL slug. Throws ResourceNotFoundException if missing.
     */
    CategoryResponse getCategoryBySlug(String slug);

    /**
     * Return a flat list of all active categories, ordered by displayOrder.
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Return top-level active categories with their subcategories nested recursively.
     * Used for navigation menus and breadcrumb generation on the frontend.
     */
    List<CategoryTreeResponse> getCategoryTree();

    /**
     * Toggle the isActive flag of a category (active ↔ inactive).
     * [ADMIN ONLY]
     */
    CategoryResponse toggleStatus(UUID id);
}
