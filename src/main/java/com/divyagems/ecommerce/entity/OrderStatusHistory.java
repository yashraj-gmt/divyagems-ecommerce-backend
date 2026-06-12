package com.divyagems.ecommerce.entity;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail of all order status transitions.
 * Each status change creates a new record — never update existing ones.
 * changedBy stores the actor (user email or "SYSTEM" for automated transitions).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_osh_order_id", columnList = "order_id")
})
public class OrderStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatusEnum status;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "changed_by", length = 150)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
