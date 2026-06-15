package com.divyagems.ecommerce.admin.dto;

import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.order.dto.response.OrderSummaryResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Complete admin dashboard statistics payload.
 * All revenue figures are from PAID orders only.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    // ─── Overview ────────────────────────────────────────────
    private long totalProducts;
    private long activeProducts;
    private long totalCategories;
    private long totalUsers;
    private long totalOrders;

    // ─── Revenue ─────────────────────────────────────────────
    private BigDecimal todayRevenue;
    private BigDecimal thisWeekRevenue;
    private BigDecimal thisMonthRevenue;
    private BigDecimal totalRevenue;

    // ─── Orders by Status ────────────────────────────────────
    /** Map of order status → count, e.g. {PENDING: 5, DELIVERED: 142, ...} */
    private Map<OrderStatusEnum, Long> ordersByStatus;

    // ─── Recent Orders ────────────────────────────────────────
    private List<OrderSummaryResponse> recentOrders;

    // ─── Top Selling Products ────────────────────────────────
    private List<TopProductResponse> topSellingProducts;

    // ─── Low Stock Alerts ────────────────────────────────────
    private List<LowStockResponse> lowStockProducts;

    // ─── Monthly Chart ───────────────────────────────────────
    /** Revenue + order count per month for the last 12 months. */
    private List<MonthlySales> monthlySalesChart;

    // ─── Users ───────────────────────────────────────────────
    private int newUsersThisMonth;
}
