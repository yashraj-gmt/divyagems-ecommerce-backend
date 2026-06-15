package com.divyagems.ecommerce.category.repository;

import com.divyagems.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();

    /** Top-level categories only (parent is null) that are active. */
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    /** All children (active or not) of a given parent. */
    List<Category> findByParent_Id(UUID parentId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    /** Counts the number of active products per category. */
    @Query("""
            SELECT c.id AS categoryId, COUNT(p.id) AS productCount
            FROM Category c
            LEFT JOIN c.products p
            GROUP BY c.id
            """)
    List<Object[]> findAllWithProductCount();

    /**
     * Check whether a category has any associated products.
     * Used before deletion to enforce the soft-delete guard.
     */
    @Query("SELECT COUNT(p.id) > 0 FROM Product p WHERE p.category.id = :categoryId OR p.subCategory.id = :categoryId")
    boolean hasProducts(@Param("categoryId") UUID categoryId);

    Optional<Category> findByName(String name);

    /**
     * Check whether a category has any active children.
     */
    @Query("SELECT COUNT(c.id) > 0 FROM Category c WHERE c.parent.id = :parentId")
    boolean hasChildren(@Param("parentId") UUID parentId);
}
