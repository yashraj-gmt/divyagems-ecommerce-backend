package com.divyagems.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a per-month running counter used to generate human-readable order IDs.
 * yearMonthKey is the natural key (e.g. "202601" = January 2026).
 * counter is incremented atomically at checkout via a PESSIMISTIC_WRITE lock.
 * This is intentionally NOT extending BaseEntity — we use the string key as identity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_monthly_counters")
public class OrderMonthlyCounter {

    /** Format: YYYYMM — e.g. "202601" for January 2026. */
    @Id
    @Column(name = "year_month_key", nullable = false, length = 7)
    private String yearMonthKey;

    /** Monotonically increasing counter for this month. */
    @Column(name = "counter", nullable = false)
    @Builder.Default
    private long counter = 0L;
}
