package com.divyagems.ecommerce.admin.dto;

import lombok.*;

import java.math.BigDecimal;

/** One data point in the monthly revenue chart (last 12 months). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySales {
    /** Short month name, e.g. "Jan", "Feb". */
    private String month;

    /** 4-digit year, e.g. 2025. */
    private int year;

    private BigDecimal revenue;
    private long       orderCount;
}
