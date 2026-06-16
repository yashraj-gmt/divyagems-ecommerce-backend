package com.divyagems.ecommerce.product.controller;

import com.divyagems.ecommerce.common.StandardApiResponse;
import com.divyagems.ecommerce.product.dto.request.BulkStatusRequest;
import com.divyagems.ecommerce.product.dto.request.ProductFilterRequest;
import com.divyagems.ecommerce.product.dto.request.ProductRequest;
import com.divyagems.ecommerce.product.dto.response.ProductPageResponse;
import com.divyagems.ecommerce.product.dto.response.ProductResponse;
import com.divyagems.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only product management endpoints.
 * All endpoints require ROLE_ADMIN.
 *
 * Full URLs (context-path = /api/v1):
 *   GET    /api/v1/admin/products
 *   POST   /api/v1/admin/products
 *   PUT    /api/v1/admin/products/{id}
 *   DELETE /api/v1/admin/products/{id}
 *   PATCH  /api/v1/admin/products/{id}/toggle-status
 *   PATCH  /api/v1/admin/products/{id}/stock
 *   POST   /api/v1/admin/products/bulk-status
 */
@Validated
@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Products (Admin)", description = "Admin product management — full CRUD, stock, bulk operations")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(
            summary = "List all products (admin view)",
            description = "Returns paginated products across all statuses. " +
                          "Supports the same filter params as the public listing " +
                          "but without defaulting to ACTIVE status."
    )
    public ResponseEntity<StandardApiResponse<ProductPageResponse>> getAllProducts(
            @ModelAttribute ProductFilterRequest filter) {

        ProductPageResponse result = productService.getProducts(filter, true);
        return ResponseEntity.ok(StandardApiResponse.success("Products fetched", result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new product [ADMIN]",
            description = "Creates a product with variants and images. " +
                          "Slug is auto-generated from name if not provided. " +
                          "SKU must be globally unique."
    )
    public ResponseEntity<StandardApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a product [ADMIN]",
            description = "Full replacement update. Variants and images are replaced (not merged). " +
                          "Slug is regenerated if name changes and no explicit slug is provided."
    )
    public ResponseEntity<StandardApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(StandardApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft-delete a product [ADMIN]",
            description = "Sets the product status to INACTIVE. " +
                          "Hard deletion is not supported to preserve order history integrity."
    )
    public ResponseEntity<StandardApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(StandardApiResponse.success("Product deactivated successfully"));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(
            summary = "Toggle product status [ADMIN]",
            description = "Flips status between ACTIVE and INACTIVE."
    )
    public ResponseEntity<StandardApiResponse<ProductResponse>> toggleStatus(@PathVariable UUID id) {
        ProductResponse result = productService.toggleStatus(id);
        String msg = result.getStatus().name().equals("ACTIVE")
                ? "Product activated"
                : "Product deactivated";
        return ResponseEntity.ok(StandardApiResponse.success(msg, result));
    }

    @PatchMapping("/{id}/stock")
    @Operation(
            summary = "Update product stock quantity [ADMIN]",
            description = "Sets the stock quantity to the provided value (absolute, not delta). " +
                          "Automatically sets status to OUT_OF_STOCK when quantity reaches 0, " +
                          "and restores to ACTIVE when quantity is positive."
    )
    public ResponseEntity<StandardApiResponse<Void>> updateStock(
            @PathVariable UUID id,
            @RequestParam @Min(value = 0, message = "Stock quantity cannot be negative") int quantity) {

        productService.updateStock(id, quantity);
        return ResponseEntity.ok(StandardApiResponse.success("Stock updated to " + quantity + " units"));
    }

    @PostMapping("/bulk-status")
    @Operation(
            summary = "Bulk update product status [ADMIN]",
            description = "Updates the status of multiple products in a single operation. " +
                          "Use for mass activate/deactivate workflows."
    )
    public ResponseEntity<StandardApiResponse<Void>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusRequest request) {

        productService.bulkUpdateStatus(request.getProductIds(), request.getStatus());
        return ResponseEntity.ok(StandardApiResponse.success(
                "Status updated to " + request.getStatus() + " for " +
                request.getProductIds().size() + " product(s)"));
    }
}
