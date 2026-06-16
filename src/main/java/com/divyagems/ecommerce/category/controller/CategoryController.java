package com.divyagems.ecommerce.category.controller;

import com.divyagems.ecommerce.category.dto.CategoryRequest;
import com.divyagems.ecommerce.category.dto.CategoryResponse;
import com.divyagems.ecommerce.category.dto.CategoryTreeResponse;
import com.divyagems.ecommerce.category.service.CategoryService;
import com.divyagems.ecommerce.common.StandardApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Category operations.
 *
 * Public endpoints  → /categories/**         (no JWT required)
 * Admin endpoints   → /admin/categories/**   (ROLE_ADMIN required)
 *
 * Full URLs (context-path = /api/v1):
 *   GET  /api/v1/categories
 *   GET  /api/v1/categories/tree
 *   GET  /api/v1/categories/{slug}
 *   POST /api/v1/admin/categories
 *   PUT  /api/v1/admin/categories/{id}
 *   DEL  /api/v1/admin/categories/{id}
 *   PATC /api/v1/admin/categories/{id}/toggle-status
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management — browse (public) and CRUD (admin)")
public class CategoryController {

    private final CategoryService categoryService;

    // ──────────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ──────────────────────────────────────────────────────────

    @GetMapping("/categories")
    @Operation(
            summary = "List all active categories",
            description = "Returns a flat list of all active categories ordered by displayOrder."
    )
    public ResponseEntity<StandardApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(StandardApiResponse.success("Categories fetched", categories));
    }

    @GetMapping("/categories/tree")
    @Operation(
            summary = "Get full category tree",
            description = "Returns top-level categories with nested subcategories. " +
                          "Ideal for navigation menus and breadcrumb generation."
    )
    public ResponseEntity<StandardApiResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(StandardApiResponse.success("Category tree fetched", tree));
    }

    @GetMapping("/categories/{slug}")
    @Operation(
            summary = "Get category by slug",
            description = "Fetch a single category by its URL-safe slug."
    )
    public ResponseEntity<StandardApiResponse<CategoryResponse>> getCategoryBySlug(
            @PathVariable String slug) {

        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(StandardApiResponse.success("Category fetched", category));
    }

    // ──────────────────────────────────────────────────────────
    // ADMIN ENDPOINTS (ROLE_ADMIN required)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Create a new category [ADMIN]",
            description = "Creates a category. Slug is auto-generated from name if not provided. " +
                          "Set parentId to create a subcategory."
    )
    public ResponseEntity<StandardApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardApiResponse.success("Category created successfully", created));
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Update a category [ADMIN]",
            description = "Updates an existing category. Slug is regenerated if the name changes " +
                          "and no explicit slug is provided."
    )
    public ResponseEntity<StandardApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(StandardApiResponse.success("Category updated successfully", updated));
    }

    @DeleteMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Delete a category [ADMIN]",
            description = "Permanently deletes a category. Returns 400 if the category has " +
                          "associated products or subcategories."
    )
    public ResponseEntity<StandardApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(StandardApiResponse.success("Category deleted successfully"));
    }

    @PatchMapping("/admin/categories/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Toggle category active status [ADMIN]",
            description = "Flips the isActive flag of a category between active and inactive."
    )
    public ResponseEntity<StandardApiResponse<CategoryResponse>> toggleStatus(@PathVariable UUID id) {
        CategoryResponse updated = categoryService.toggleStatus(id);
        String msg = updated.isActive()
                ? "Category activated successfully"
                : "Category deactivated successfully";
        return ResponseEntity.ok(StandardApiResponse.success(msg, updated));
    }
}
