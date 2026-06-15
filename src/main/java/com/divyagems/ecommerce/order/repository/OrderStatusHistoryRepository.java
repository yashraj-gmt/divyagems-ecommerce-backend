package com.divyagems.ecommerce.order.repository;

import com.divyagems.ecommerce.entity.Order;
import com.divyagems.ecommerce.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    /** Chronological audit trail — oldest first for timeline display. */
    List<OrderStatusHistory> findByOrderOrderByChangedAtAsc(Order order);
}
