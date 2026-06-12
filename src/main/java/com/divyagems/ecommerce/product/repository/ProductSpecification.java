package com.divyagems.ecommerce.product.repository;

import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.product.dto.request.ProductFilterRequest;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Specification builder for dynamic Product filtering.
 *
 * Design principles:
 * - Each predicate is added only if the corresponding filter field is non-null/non-empty.
 * - Tag-type based filters (materials, planets, chakras, elements) are pre-resolved to
 *   tag IDs in the service layer and merged into resolvedTagIds before calling build().
 * - query.distinct(true) prevents duplicate results when joining collection associations.
 * - Effective price uses COALESCE(salePrice, price) for range filtering.
 */
public class ProductSpecification {

    private ProductSpecification() {}

    /**
     * Build a combined Specification from the filter request and pre-resolved tag IDs.
     *
     * @param filter         the filter DTO from the request
     * @param resolvedTagIds tag IDs resolved from materials/planets/chakras/elements filters
     * @param adminMode      if true, don't default-restrict by ACTIVE status
     */
    public static Specification<Product> build(
            ProductFilterRequest filter,
            Set<UUID> resolvedTagIds,
            boolean adminMode) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always distinct when joining collection associations
            query.distinct(true);

            // ── Status ─────────────────────────────────────────────────────
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            } else if (!adminMode) {
                // Public: only show ACTIVE products by default
                predicates.add(cb.equal(root.get("status"), ProductStatusEnum.ACTIVE));
            }

            // ── Keyword (ILIKE on name, shortDescription, sku) ─────────────
            if (StringUtils.hasText(filter.getKeyword())) {
                String pattern = "%" + filter.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("shortDescription")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }

            // ── Category by slug ───────────────────────────────────────────
            if (StringUtils.hasText(filter.getCategorySlug())) {
                Join<Object, Object> catJoin = root.join("category", JoinType.INNER);
                predicates.add(cb.equal(catJoin.get("slug"), filter.getCategorySlug()));
            }

            // ── Sub-category by slug ───────────────────────────────────────
            if (StringUtils.hasText(filter.getSubCategorySlug())) {
                Join<Object, Object> subCatJoin = root.join("subCategory", JoinType.LEFT);
                predicates.add(cb.equal(subCatJoin.get("slug"), filter.getSubCategorySlug()));
            }

            // ── Effective price range: COALESCE(salePrice, price) ──────────
            if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
                Expression<BigDecimal> effectivePrice =
                        cb.coalesce(root.get("salePrice"), root.get("price"));

                if (filter.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(effectivePrice, filter.getMinPrice()));
                }
                if (filter.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(effectivePrice, filter.getMaxPrice()));
                }
            }

            // ── Tags (explicit tagIds + resolved type-based tag IDs) ───────
            if (!CollectionUtils.isEmpty(resolvedTagIds)) {
                // Use subquery to avoid duplicate rows from multiple tag joins
                Subquery<UUID> tagSubquery = query.subquery(UUID.class);
                Root<Product> tagSubRoot = tagSubquery.from(Product.class);
                Join<Object, Object> tagJoin = tagSubRoot.join("tags");
                tagSubquery.select(tagSubRoot.get("id"))
                        .where(
                                cb.equal(tagSubRoot.get("id"), root.get("id")),
                                tagJoin.get("id").in(resolvedTagIds)
                        );
                predicates.add(cb.exists(tagSubquery));
            }

            // ── Color names (OR within list) ───────────────────────────────
            if (!CollectionUtils.isEmpty(filter.getColorNames())) {
                Subquery<UUID> variantSubquery = query.subquery(UUID.class);
                Root<Product> variantSubRoot = variantSubquery.from(Product.class);
                Join<Object, Object> variantJoin = variantSubRoot.join("variants");
                variantSubquery.select(variantSubRoot.get("id"))
                        .where(
                                cb.equal(variantSubRoot.get("id"), root.get("id")),
                                variantJoin.get("colorName").in(filter.getColorNames()),
                                cb.isTrue(variantJoin.get("isActive"))
                        );
                predicates.add(cb.exists(variantSubquery));
            }

            // ── Boolean flags ──────────────────────────────────────────────
            if (filter.getIsFeatured() != null) {
                predicates.add(cb.equal(root.get("isFeatured"), filter.getIsFeatured()));
            }
            if (filter.getIsNewArrival() != null) {
                predicates.add(cb.equal(root.get("isNewArrival"), filter.getIsNewArrival()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
