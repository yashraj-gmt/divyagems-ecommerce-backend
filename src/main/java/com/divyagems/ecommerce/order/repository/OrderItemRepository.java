package com.divyagems.ecommerce.order.repository;

import com.divyagems.ecommerce.entity.Order;
import com.divyagems.ecommerce.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /** Ordered chronologically for consistent display. */
    List<OrderItem> findByOrderOrderByCreatedAtAsc(Order order);

    /**
     * Top N products by units sold (PAID orders only).
     * Returns Object[]{productName(String), totalSold(Long), revenue(BigDecimal)}.
     */
    @Query("""
            SELECT oi.productName,
                   SUM(oi.quantity),
                   SUM(oi.totalPrice)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.paymentStatus = com.divyagems.ecommerce.enums.PaymentStatusEnum.PAID
            GROUP BY oi.product.id, oi.productName
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingProducts(Pageable pageable);
}

