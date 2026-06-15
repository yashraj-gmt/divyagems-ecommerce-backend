package com.divyagems.ecommerce.product.repository;

import com.divyagems.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProduct_Id(UUID productId);

    List<ProductVariant> findByProduct_IdAndIsActiveTrue(UUID productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    /**
     * Distinct active color names + hex codes for filter options.
     * Returns Object[]{colorName, colorHexCode} rows — nulls excluded.
     */
    @Query("""
            SELECT DISTINCT v.colorName, v.colorHexCode
            FROM ProductVariant v
            WHERE v.isActive = true
              AND v.colorName IS NOT NULL
            ORDER BY v.colorName
            """)
    List<Object[]> findDistinctActiveColors();
}

