package com.divyagems.ecommerce.admin.service;

import com.divyagems.ecommerce.admin.dto.*;
import com.divyagems.ecommerce.category.repository.CategoryRepository;
import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.order.dto.response.OrderSummaryResponse;
import com.divyagems.ecommerce.order.repository.OrderItemRepository;
import com.divyagems.ecommerce.order.repository.OrderRepository;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository    orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        // ─── Overview Counts ─────────────────────────────────
        long totalProducts    = productRepository.count();
        long activeProducts   = productRepository.countByStatus(ProductStatusEnum.ACTIVE);
        long totalCategories  = categoryRepository.count();
        long totalUsers       = userRepository.count();
        long totalOrders      = orderRepository.count();

        // ─── Revenue ─────────────────────────────────────────
        LocalDateTime now          = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday   = startOfToday.plusDays(1);

        LocalDateTime startOfWeek  = now.toLocalDate()
                .with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        BigDecimal todayRevenue     = nullSafe(orderRepository.sumRevenueByDateRange(startOfToday, endOfToday));
        BigDecimal thisWeekRevenue  = nullSafe(orderRepository.sumRevenueByDateRange(startOfWeek, now));
        BigDecimal thisMonthRevenue = nullSafe(orderRepository.sumRevenueByDateRange(startOfMonth, now));
        BigDecimal totalRevenue     = nullSafe(orderRepository.sumTotalRevenue());

        // ─── Orders by Status ─────────────────────────────────
        Map<OrderStatusEnum, Long> ordersByStatus = new EnumMap<>(OrderStatusEnum.class);
        // Pre-fill all statuses with 0
        for (OrderStatusEnum s : OrderStatusEnum.values()) ordersByStatus.put(s, 0L);
        orderRepository.countByOrderStatus().forEach(row -> {
            OrderStatusEnum status = (OrderStatusEnum) row[0];
            long            count  = ((Number) row[1]).longValue();
            ordersByStatus.put(status, count);
        });

        // ─── Recent Orders (last 10) ──────────────────────────
        List<OrderSummaryResponse> recentOrders = orderRepository
                .findTop10ByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream()
                .map(o -> OrderSummaryResponse.builder()
                        .orderId(o.getOrderId())
                        .status(o.getOrderStatus())
                        .paymentStatus(o.getPaymentStatus())
                        .totalAmount(o.getTotalAmount())
                        .itemCount(o.getOrderItems().size())
                        .createdAt(o.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // ─── Top Selling Products (top 5) ─────────────────────
        List<TopProductResponse> topSelling = orderItemRepository
                .findTopSellingProducts(PageRequest.of(0, 5))
                .stream()
                .map(row -> TopProductResponse.builder()
                        .productName((String) row[0])
                        .totalSold(((Number) row[1]).longValue())
                        .revenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        // ─── Low Stock Products ───────────────────────────────
        List<LowStockResponse> lowStock = productRepository.findLowStockProducts()
                .stream()
                .map(p -> LowStockResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .sku(p.getSku())
                        .stockQuantity(p.getStockQuantity())
                        .lowStockThreshold(p.getLowStockThreshold())
                        .build())
                .collect(Collectors.toList());

        // ─── Monthly Sales Chart (last 12 months) ────────────
        List<MonthlySales> monthlySalesChart = orderRepository.findMonthlySales()
                .stream()
                .map(row -> {
                    int        yr    = ((Number) row[0]).intValue();
                    int        mo    = ((Number) row[1]).intValue();
                    BigDecimal rev   = new BigDecimal(row[2].toString());
                    long       count = ((Number) row[3]).longValue();
                    String monthName = Month.of(mo)
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    return MonthlySales.builder()
                            .year(yr)
                            .month(monthName)
                            .revenue(rev)
                            .orderCount(count)
                            .build();
                })
                .collect(Collectors.toList());

        // ─── New Users This Month ─────────────────────────────
        int newUsersThisMonth = (int) userRepository.countByCreatedAtAfter(startOfMonth);

        return DashboardStatsResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalCategories(totalCategories)
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .todayRevenue(todayRevenue)
                .thisWeekRevenue(thisWeekRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .totalRevenue(totalRevenue)
                .ordersByStatus(ordersByStatus)
                .recentOrders(recentOrders)
                .topSellingProducts(topSelling)
                .lowStockProducts(lowStock)
                .monthlySalesChart(monthlySalesChart)
                .newUsersThisMonth(newUsersThisMonth)
                .build();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
