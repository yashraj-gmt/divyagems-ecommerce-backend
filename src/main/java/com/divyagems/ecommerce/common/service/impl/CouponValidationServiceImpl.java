package com.divyagems.ecommerce.common.service.impl;

import com.divyagems.ecommerce.common.service.CouponValidationResult;
import com.divyagems.ecommerce.common.service.CouponValidationService;
import com.divyagems.ecommerce.entity.Coupon;
import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.enums.DiscountTypeEnum;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.order.repository.CouponRepository;
import com.divyagems.ecommerce.order.repository.OrderRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponValidationServiceImpl implements CouponValidationService {

    private final CouponRepository couponRepository;
    private final OrderRepository  orderRepository;
    private final UserRepository   userRepository;

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResult validateAndApplyCoupon(String code, BigDecimal orderAmount, UUID userId) {

        if (code == null || code.isBlank()) {
            return invalid("Coupon code must not be blank.", null);
        }

        // 1. Exists?
        Optional<Coupon> opt = couponRepository.findByCodeAndIsActiveTrue(code.trim().toUpperCase());
        if (opt.isEmpty()) {
            return invalid("Coupon code '" + code + "' is invalid or inactive.", null);
        }
        Coupon coupon = opt.get();

        // 2. Not expired (validFrom ≤ now ≤ validUntil)
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            return invalid("Coupon '" + code + "' has expired.", coupon);
        }

        // 3. Minimum order amount met?
        if (orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return invalid(
                    "Minimum order amount for this coupon is ₹" +
                            coupon.getMinimumOrderAmount().toPlainString() + ".",
                    coupon
            );
        }

        // 4. Global usage limit not exceeded? (0 = unlimited)
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return invalid("Coupon '" + code + "' has reached its usage limit.", coupon);
        }

        // 5. One-time-per-user check
        if (coupon.isOneTimePerUser()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            boolean alreadyUsed = orderRepository.existsByUserAndCouponCode(user, coupon.getCode());
            if (alreadyUsed) {
                return invalid("You have already used this coupon.", coupon);
            }
        }

        // 6. Calculate discount
        BigDecimal discount = calculateDiscount(coupon, orderAmount);

        log.info("Coupon '{}' validated successfully — discount: ₹{}", code, discount);
        return CouponValidationResult.builder()
                .isValid(true)
                .discount(discount)
                .message("Coupon applied successfully.")
                .coupon(coupon)
                .build();
    }

    // ─── Private Helpers ───────────────────────────────────────

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountTypeEnum.PERCENTAGE) {
            // discount = orderAmount * discountValue / 100
            discount = orderAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Cap if maximumDiscountAmount is set
            if (coupon.getMaximumDiscountAmount() != null &&
                    discount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                discount = coupon.getMaximumDiscountAmount();
            }
        } else {
            // FLAT — discount is the raw value, can't exceed order amount
            discount = coupon.getDiscountValue().min(orderAmount);
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private CouponValidationResult invalid(String message, Coupon coupon) {
        return CouponValidationResult.builder()
                .isValid(false)
                .discount(BigDecimal.ZERO)
                .message(message)
                .coupon(coupon)
                .build();
    }
}
