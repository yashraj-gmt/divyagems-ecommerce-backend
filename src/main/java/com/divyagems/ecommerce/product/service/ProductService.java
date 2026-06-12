package com.divyagems.ecommerce.product.service;

import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.product.dto.request.ProductFilterRequest;
import com.divyagems.ecommerce.product.dto.request.ProductRequest;
import com.divyagems.ecommerce.product.dto.response.ProductPageResponse;
import com.divyagems.ecommerce.product.dto.response.ProductResponse;
import com.divyagems.ecommerce.product.dto.response.ProductSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    /** Create a new product with variants and images. [ADMIN] */
    ProductResponse createProduct(ProductRequest request);

    /** Full update of a product (replaces variants and images). [ADMIN] */
    ProductResponse updateProduct(UUID id, ProductRequest request);

    /**
     * Soft-delete: sets status to INACTIVE.
     * Hard deletion is not allowed to preserve order history integrity.
     * [ADMIN]
     */
    void deleteProduct(UUID id);

    /** Fetch full product detail by UUID. Throws ResourceNotFoundException if missing. */
    ProductResponse getProductById(UUID id);

    /** Fetch full product detail by URL slug. Throws ResourceNotFoundException if missing. */
    ProductResponse getProductBySlug(String slug);

    /**
     * Paginated and filtered product listing.
     *
     * @param filter   filter and sort parameters from the request
     * @param adminMode true = include all statuses; false = ACTIVE only
     */
    ProductPageResponse getProducts(ProductFilterRequest filter, boolean adminMode);

    /** Products marked isFeatured=true, sorted by rating. */
    List<ProductSummaryResponse> getFeaturedProducts();

    /** Latest products marked isNewArrival=true. */
    List<ProductSummaryResponse> getNewArrivals();

    /**
     * Up to 8 products from the same category, excluding the current product.
     * Sorted by rating DESC.
     */
    List<ProductSummaryResponse> getRelatedProducts(UUID productId);

    /**
     * Adjust stock quantity (absolute set, not delta).
     * [ADMIN]
     */
    void updateStock(UUID productId, int quantity);

    /** Toggle product status: ACTIVE ↔ INACTIVE. [ADMIN] */
    ProductResponse toggleStatus(UUID id);

    /** Batch status update for multiple products. [ADMIN] */
    void bulkUpdateStatus(List<UUID> ids, ProductStatusEnum status);
}
