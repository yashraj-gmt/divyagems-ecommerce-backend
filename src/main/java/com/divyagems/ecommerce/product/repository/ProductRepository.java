package com.divyagems.ecommerce.product.repository;

import com.divyagems.ecommerce.entity.Category;
import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.enums.ProductStatusEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    List<Product> findByStatusAndIsFeaturedTrueOrderByAverageRatingDesc(ProductStatusEnum status);

    /**
     * BUG-005 fix: Spring Data rejects a method with both an OrderBy clause in the
     * name AND a Pageable parameter. Removed OrderBy from the method name;
     * sort direction (createdAt DESC) is applied via the Pageable passed from the service.
     */
    List<Product> findByStatusAndIsNewArrivalTrue(ProductStatusEnum status, Pageable pageable);

    /**
     * Related products: same category, different product, ACTIVE status, ordered by rating.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.category = :category
              AND p.id != :excludeId
              AND p.status = com.divyagems.ecommerce.enums.ProductStatusEnum.ACTIVE
            ORDER BY p.averageRating DESC, p.totalReviews DESC
            """)
    List<Product> findRelatedProducts(
            @Param("category") Category category,
            @Param("excludeId") UUID excludeId,
            Pageable pageable);

    /**
     * Bulk update status for a list of product IDs.
     */
    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("status") ProductStatusEnum status);
}
