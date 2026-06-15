package com.divyagems.ecommerce.review.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public review response.
 * user field shows firstName + last-name initial (e.g. "Priya S.") for privacy.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private UUID id;
    private int rating;
    private String title;
    private String body;

    /** Display name: "FirstName L." — last name is initial only. */
    private String userName;

    private boolean isVerifiedPurchase;
    private boolean isApproved;
    private int helpfulCount;
    private LocalDateTime createdAt;
}
