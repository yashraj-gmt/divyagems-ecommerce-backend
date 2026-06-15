package com.divyagems.ecommerce.review.repository;

import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.entity.Review;
import com.divyagems.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Public listing: approved reviews for a product, paginated. */
    Page<Review> findByProductAndIsApprovedTrueOrderByCreatedAtDesc(Product product, Pageable pageable);

    /** Rating distribution for approved reviews of a product. */
    @Query("""
            SELECT r.rating, COUNT(r)
            FROM Review r
            WHERE r.product = :product AND r.isApproved = true
            GROUP BY r.rating
            ORDER BY r.rating DESC
            """)
    java.util.List<Object[]> findRatingDistribution(@Param("product") Product product);

    /** Average rating across approved reviews (null if none). */
    @Query("""
            SELECT COALESCE(AVG(CAST(r.rating AS double)), 0.0),
                   COUNT(r)
            FROM Review r
            WHERE r.product = :product AND r.isApproved = true
            """)
    Object[] findAverageRatingAndCount(@Param("product") Product product);

    /** One-review-per-product check. */
    boolean existsByProductAndUser(Product product, User user);

    /**
     * Verified-purchase check: has the user received a DELIVERED order containing this product?
     * OrderStatus.DELIVERED = 'DELIVERED'.
     */
    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.product = :product
              AND o.user = :user
              AND o.status = com.divyagems.ecommerce.enums.OrderStatusEnum.DELIVERED
            """)
    boolean isVerifiedPurchaser(@Param("product") Product product, @Param("user") User user);
}
