package com.divyagems.ecommerce.common.service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contract for coupon validation and discount calculation.
 * Must be called before order creation; NOT responsible for incrementing usedCount —
 * that happens inside the order creation transaction.
 */
public interface CouponValidationService {

    /**
     * Validate a coupon code and calculate the discount to apply.
     *
     * @param code        the coupon code entered by the user (case-insensitive)
     * @param orderAmount the subtotal amount (before discount / shipping)
     * @param userId      the authenticated user's UUID (for one-time-per-user checks)
     * @return CouponValidationResult with isValid, discount amount, and reason message
     */
    CouponValidationResult validateAndApplyCoupon(String code, BigDecimal orderAmount, UUID userId);
}
