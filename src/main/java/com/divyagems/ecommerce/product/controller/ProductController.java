package com.divyagems.ecommerce.product.controller;

import com.divyagems.ecommerce.common.ApiResponse;
import com.divyagems.ecommerce.product.dto.request.ProductFilterRequest;
import com.divyagems.ecommerce.product.dto.response.ProductPageResponse;
import com.divyagems.ecommerce.product.dto.response.ProductResponse;
import com.divyagems.ecommerce.product.dto.response.ProductSummaryResponse;
import com.divyagems.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public-facing product endpoints (no authentication required).
 *
 * Full URLs (context-path = /api/v1):
 *   GET /api/v1/products
 *   GET /api/v1/products/featured
 *   GET /api/v1/products/new-arrivals
 *   GET /api/v1/products/{slug}
 *   GET /api/v1/products/{id}/related
 *
 * Note: /products/featured and /products/new-arrivals MUST be mapped
 * before /products/{slug} to avoid path ambiguity (Spring resolves in order).
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products (Public)", description = "Public product catalogue endpoints — browse, search, and filter")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(
            summary = "Search and filter products",
            description = "Paginated product listing with full filtering: keyword, category, price range, " +
                          "colors, spiritual properties (materials, planets, chakras, elements), and sorting. " +
                          "Only ACTIVE products are returned. Use @ModelAttribute binding for multi-value params."
    )
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts(
            @ModelAttribute ProductFilterRequest filter) {

        ProductPageResponse result = productService.getProducts(filter, false);
        return ResponseEntity.ok(ApiResponse.success("Products fetched", result));
    }

    @GetMapping("/featured")
    @Operation(
            summary = "Get featured products",
            description = "Returns all active products marked as featured, sorted by rating descending."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getFeaturedProducts() {
        List<ProductSummaryResponse> result = productService.getFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success("Featured products fetched", result));
    }

    @GetMapping("/new-arrivals")
    @Operation(
            summary = "Get new arrivals",
            description = "Returns the latest 12 active products marked as new arrivals, sorted by creation date."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getNewArrivals() {
        List<ProductSummaryResponse> result = productService.getNewArrivals();
        return ResponseEntity.ok(ApiResponse.success("New arrivals fetched", result));
    }

    @GetMapping("/{slug}")
    @Operation(
            summary = "Get product detail by slug",
            description = "Returns full product details including variants, images, tags grouped by type, " +
                          "and SEO metadata. Slug is the URL-safe identifier (e.g. 'ruby-gemstone-pendant')."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(
            @PathVariable String slug) {

        ProductResponse result = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Product fetched", result));
    }

    @GetMapping("/{id}/related")
    @Operation(
            summary = "Get related products",
            description = "Returns up to 8 active products from the same category, " +
                          "sorted by rating. Excludes the current product."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getRelatedProducts(
            @PathVariable UUID id) {

        List<ProductSummaryResponse> result = productService.getRelatedProducts(id);
        return ResponseEntity.ok(ApiResponse.success("Related products fetched", result));
    }
}
