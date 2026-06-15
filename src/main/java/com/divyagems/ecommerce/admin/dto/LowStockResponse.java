package com.divyagems.ecommerce.admin.dto;

import lombok.*;

import java.util.UUID;

/** Low-stock product alert for dashboard widget. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockResponse {
    private UUID   id;
    private String name;
    private String sku;
    private int    stockQuantity;
    private int    lowStockThreshold;
}
