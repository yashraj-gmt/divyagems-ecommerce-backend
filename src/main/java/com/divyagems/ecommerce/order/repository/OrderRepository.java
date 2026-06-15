package com.divyagems.ecommerce.order.repository;

import com.divyagems.ecommerce.entity.Order;
import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    /** Find by human-readable orderId (e.g. DG-202601-00001). */
    Optional<Order> findByOrderId(String orderId);

    /** Paginated order list for authenticated user — newest first. */
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /** Find by orderId AND user — used to enforce ownership. */
    Optional<Order> findByOrderIdAndUser(String orderId, User user);

    /** Check if a user has already used a specific coupon code. */
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.user = :user AND o.couponCode = :couponCode")
    boolean existsByUserAndCouponCode(@Param("user") User user, @Param("couponCode") String couponCode);

    /** Find by razorpay order ID for payment callback reconciliation. */
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    /** All orders filtered by status (admin use). */
    Page<Order> findByOrderStatusOrderByCreatedAtDesc(OrderStatusEnum status, Pageable pageable);

    /** All orders filtered by payment status (admin use). */
    Page<Order> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatusEnum paymentStatus, Pageable pageable);

    // ─── Dashboard Aggregation Queries ────────────────────────

    /** Total revenue from PAID orders in a date range. */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.paymentStatus = com.divyagems.ecommerce.enums.PaymentStatusEnum.PAID
              AND o.createdAt >= :start AND o.createdAt < :end
            """)
    BigDecimal sumRevenueByDateRange(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /** Total all-time revenue from PAID orders. */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.paymentStatus = com.divyagems.ecommerce.enums.PaymentStatusEnum.PAID
            """)
    BigDecimal sumTotalRevenue();

    /** Count of orders grouped by orderStatus. Returns Object[]{OrderStatusEnum, Long}. */
    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o GROUP BY o.orderStatus")
    List<Object[]> countByOrderStatus();

    /** 10 most recent orders for admin dashboard. */
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Monthly revenue + order count for the last N months (PAID orders only).
     * Returns Object[]{year(int), month(int), revenue(BigDecimal), orderCount(Long)}.
     */
    @Query(value = """
            SELECT EXTRACT(YEAR  FROM created_at)::int AS yr,
                   EXTRACT(MONTH FROM created_at)::int AS mo,
                   COALESCE(SUM(total_amount), 0)      AS revenue,
                   COUNT(*)                            AS order_count
            FROM orders
            WHERE payment_status = 'PAID'
              AND created_at >= NOW() - INTERVAL '12 months'
            GROUP BY yr, mo
            ORDER BY yr, mo
            """, nativeQuery = true)
    List<Object[]> findMonthlySales();
}

