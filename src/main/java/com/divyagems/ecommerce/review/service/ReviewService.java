package com.divyagems.ecommerce.review.service;

import com.divyagems.ecommerce.review.dto.request.CreateReviewRequest;
import com.divyagems.ecommerce.review.dto.response.ProductRatingSummary;
import com.divyagems.ecommerce.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    /**
     * Submit a review for a product.
     * Guards: user must have a DELIVERED order containing the product;
     * one review per user per product.
     * Review starts in pending (isApproved=false) state — admin must approve it.
     */
    ReviewResponse createReview(UUID productId, CreateReviewRequest request);

    /**
     * Public paginated listing of APPROVED reviews for a product.
     * Sorted by createdAt DESC by default.
     */
    Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable);

    /**
     * Aggregate rating summary: averageRating, totalReviews, and 1-5 distribution.
     * Only approved reviews are counted.
     */
    ProductRatingSummary getProductRatingSummary(UUID productId);

    /**
     * Increment the helpful count of a review.
     * Authenticated users only — no duplicate-helpful guard in v1.
     */
    void markHelpful(UUID reviewId);

    /** [ADMIN] Approve a pending review and recalculate product stats. */
    ReviewResponse approveReview(UUID reviewId);

    /**
     * [ADMIN or OWNER] Delete a review.
     * If approved, recalculate product averageRating + totalReviews after deletion.
     */
    void deleteReview(UUID reviewId);
}
