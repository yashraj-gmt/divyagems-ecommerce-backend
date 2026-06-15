package com.divyagems.ecommerce.review.dto.response;

import lombok.*;

import java.util.Map;

/**
 * Product rating summary for the review section header.
 * ratingDistribution maps star count → number of reviews: {5: 12, 4: 8, 3: 2, 2: 0, 1: 1}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingSummary {

    private double averageRating;
    private long totalReviews;

    /**
     * Distribution of ratings from 1 to 5.
     * All five keys are always present (missing counts default to 0).
     */
    private Map<Integer, Long> ratingDistribution;
}
