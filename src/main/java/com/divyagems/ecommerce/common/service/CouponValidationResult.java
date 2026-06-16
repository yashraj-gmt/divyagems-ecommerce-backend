package com.divyagems.ecommerce.common.service;

import com.divyagems.ecommerce.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Result of a coupon validation check.
 * If {@code isValid} is false, {@code discount} will be {@link BigDecimal#ZERO}
 * and {@code message} will contain the reason for rejection.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidationResult {
    private boolean isValid;
    private BigDecimal discount;
    private String message;
    private Coupon coupon;
}
