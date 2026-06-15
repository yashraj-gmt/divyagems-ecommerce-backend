package com.divyagems.ecommerce.review.controller;

import com.divyagems.ecommerce.review.dto.request.CreateReviewRequest;
import com.divyagems.ecommerce.review.dto.response.ProductRatingSummary;
import com.divyagems.ecommerce.review.dto.response.ReviewResponse;
import com.divyagems.ecommerce.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Review endpoints.
 *
 * Public:  GET /products/{productId}/reviews
 *          GET /products/{productId}/reviews/summary
 * Auth:    POST /products/{productId}/reviews
 *          POST /reviews/{id}/helpful
 * Admin:   PATCH /admin/reviews/{id}/approve
 *          DELETE /admin/reviews/{id}
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews — create, read, moderate")
public class ReviewController {

    private final ReviewService reviewService;

    // ─── GET /products/{productId}/reviews ────────────────────
    @Operation(summary = "List approved reviews for a product (public)")
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }

    // ─── GET /products/{productId}/reviews/summary ────────────
    @Operation(summary = "Get rating summary and distribution for a product (public)")
    @GetMapping("/products/{productId}/reviews/summary")
    public ResponseEntity<ProductRatingSummary> getRatingSummary(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.getProductRatingSummary(productId));
    }

    // ─── POST /products/{productId}/reviews ───────────────────
    @Operation(summary = "Submit a review (must have a DELIVERED order with this product)")
    @PostMapping("/products/{productId}/reviews")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(productId, request));
    }

    // ─── POST /reviews/{id}/helpful ───────────────────────────
    @Operation(summary = "Mark a review as helpful")
    @PostMapping("/reviews/{id}/helpful")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> markHelpful(@PathVariable UUID id) {
        reviewService.markHelpful(id);
        return ResponseEntity.ok().build();
    }

    // ─── PATCH /admin/reviews/{id}/approve ────────────────────
    @Operation(summary = "[ADMIN] Approve a pending review")
    @PatchMapping("/admin/reviews/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    // ─── DELETE /admin/reviews/{id} ───────────────────────────
    @Operation(summary = "[ADMIN] Delete any review")
    @DeleteMapping("/admin/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
