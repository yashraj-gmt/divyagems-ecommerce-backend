package com.divyagems.ecommerce.review.service.impl;

import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.entity.Review;
import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import com.divyagems.ecommerce.review.dto.request.CreateReviewRequest;
import com.divyagems.ecommerce.review.dto.response.ProductRatingSummary;
import com.divyagems.ecommerce.review.dto.response.ReviewResponse;
import com.divyagems.ecommerce.review.repository.ReviewRepository;
import com.divyagems.ecommerce.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository  reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;

    // ─── Create Review ─────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse createReview(UUID productId, CreateReviewRequest request) {
        User    user    = requireCurrentUser();
        Product product = findProductOrThrow(productId);

        // One-review-per-user-per-product guard
        if (reviewRepository.existsByProductAndUser(product, user)) {
            throw new BusinessException("You have already reviewed this product.");
        }

        // Verified-purchase check (DELIVERED order required)
        boolean hasDeliveredOrder = reviewRepository.isVerifiedPurchaser(product, user);
        if (!hasDeliveredOrder) {
            throw new BusinessException(
                    "You can only review products from a delivered order.");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .isVerifiedPurchase(true)
                .isApproved(false)  // Requires admin approval before going live
                .helpfulCount(0)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review submitted by {} for product {} (pending approval)", user.getEmail(), productId);
        return mapToResponse(saved);
    }

    // ─── Get Product Reviews ───────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        Product product = findProductOrThrow(productId);
        return reviewRepository
                .findByProductAndIsApprovedTrueOrderByCreatedAtDesc(product, pageable)
                .map(this::mapToResponse);
    }

    // ─── Rating Summary ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummary getProductRatingSummary(UUID productId) {
        Product product = findProductOrThrow(productId);

        // Aggregate avg + count
        Object[] agg = reviewRepository.findAverageRatingAndCount(product);
        double avg   = agg[0] == null ? 0.0 : ((Number) agg[0]).doubleValue();
        long   total = agg[1] == null ? 0L  : ((Number) agg[1]).longValue();
        avg = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // Rating distribution: initialise all 5 stars to 0
        Map<Integer, Long> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);

        List<Object[]> rawDist = reviewRepository.findRatingDistribution(product);
        for (Object[] row : rawDist) {
            int  star  = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            dist.put(star, count);
        }

        return ProductRatingSummary.builder()
                .averageRating(avg)
                .totalReviews(total)
                .ratingDistribution(dist)
                .build();
    }

    // ─── Mark Helpful ──────────────────────────────────────────

    @Override
    @Transactional
    public void markHelpful(UUID reviewId) {
        requireCurrentUser(); // must be authenticated
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
    }

    // ─── Approve Review [ADMIN] ────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse approveReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (review.isApproved()) {
            return mapToResponse(review); // idempotent
        }

        review.setApproved(true);
        reviewRepository.save(review);

        // Recalculate product stats now that one more review is approved
        recalculateProductStats(review.getProduct());

        log.info("Review {} approved, product {} stats recalculated", reviewId, review.getProduct().getId());
        return mapToResponse(review);
    }

    // ─── Delete Review [ADMIN or OWNER] ───────────────────────

    @Override
    @Transactional
    public void deleteReview(UUID reviewId) {
        User   caller = requireCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        boolean isAdmin = caller.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        boolean isOwner = review.getUser().getId().equals(caller.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to delete this review.");
        }

        boolean wasApproved = review.isApproved();
        Product product     = review.getProduct();

        reviewRepository.delete(review);

        // Only recalculate if the deleted review was approved (it affected public stats)
        if (wasApproved) {
            recalculateProductStats(product);
        }

        log.info("Review {} deleted by {} (was approved: {})", reviewId, caller.getEmail(), wasApproved);
    }

    // ─── Private — Product Stat Recalculation ─────────────────

    /**
     * Re-computes averageRating and totalReviews on the Product entity
     * from the current set of approved reviews.
     * Called after approve or delete to keep the Product in sync.
     */
    private void recalculateProductStats(Product product) {
        Object[] agg   = reviewRepository.findAverageRatingAndCount(product);
        double   avg   = agg[0] == null ? 0.0 : ((Number) agg[0]).doubleValue();
        long     count = agg[1] == null ? 0L  : ((Number) agg[1]).longValue();

        avg = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue();

        product.setAverageRating(avg);
        product.setTotalReviews((int) count);
        productRepository.save(product);

        log.debug("Product {} stats updated: avg={}, total={}", product.getId(), avg, count);
    }

    // ─── Private — Mapper ──────────────────────────────────────

    private ReviewResponse mapToResponse(Review r) {
        String userName = buildUserDisplayName(r.getUser());
        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .userName(userName)
                .isVerifiedPurchase(r.isVerifiedPurchase())
                .isApproved(r.isApproved())
                .helpfulCount(r.getHelpfulCount())
                .createdAt(r.getCreatedAt())
                .build();
    }

    /**
     * Build a privacy-safe display name: "FirstName L."
     * where L is the first character of the last name.
     */
    private String buildUserDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last  = user.getLastName()  != null && !user.getLastName().isEmpty()
                ? user.getLastName().substring(0, 1).toUpperCase() + "."
                : "";
        return (first + " " + last).trim();
    }

    // ─── Private — Helpers ─────────────────────────────────────

    private Product findProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", auth.getName()));
    }
}
