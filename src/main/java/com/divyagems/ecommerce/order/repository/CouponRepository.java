package com.divyagems.ecommerce.order.repository;

import com.divyagems.ecommerce.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    /** Lookup active coupon by code (case-sensitive). */
    Optional<Coupon> findByCodeAndIsActiveTrue(String code);
}
