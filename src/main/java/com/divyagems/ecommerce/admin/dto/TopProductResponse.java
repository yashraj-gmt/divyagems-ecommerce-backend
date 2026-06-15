package com.divyagems.ecommerce.admin.dto;

import lombok.*;

import java.math.BigDecimal;

/** Top-selling product summary for dashboard widget. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductResponse {
    private String productName;
    private long   totalSold;
    private BigDecimal revenue;
}
