package com.divyagems.ecommerce.order.controller;

import com.divyagems.ecommerce.order.dto.request.OrderFilterRequest;
import com.divyagems.ecommerce.order.dto.request.UpdateOrderStatusRequest;
import com.divyagems.ecommerce.order.dto.response.OrderResponse;
import com.divyagems.ecommerce.order.dto.response.OrderSummaryResponse;
import com.divyagems.ecommerce.order.service.OrderService;
import com.divyagems.ecommerce.enums.OrderStatusEnum;
import com.divyagems.ecommerce.enums.PaymentMethodEnum;
import com.divyagems.ecommerce.enums.PaymentStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Admin-only order management endpoints.
 * All methods are protected by {@code @PreAuthorize("hasRole('ADMIN')")} in addition
 * to the {@code /admin/**} path-level security rule in SecurityConfig.
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Orders", description = "Admin order management: view, filter, update status, cancel, export CSV")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final OrderService orderService;

    // ─── GET /admin/orders ────────────────────────────────────
    @Operation(summary = "List all orders with optional filtering (admin)")
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatusEnum status,
            @RequestParam(required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(required = false) PaymentMethodEnum paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir) {

        OrderFilterRequest filter = OrderFilterRequest.builder()
                .status(status)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .fromDate(fromDate)
                .toDate(toDate)
                .search(search)
                .build();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.getAllOrders(filter, pageable));
    }

    // ─── GET /admin/orders/export ─────────────────────────────
    @Operation(summary = "Export all (filtered) orders to CSV")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrdersCsv(
            @RequestParam(required = false) OrderStatusEnum status,
            @RequestParam(required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(required = false) PaymentMethodEnum paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search) {

        OrderFilterRequest filter = OrderFilterRequest.builder()
                .status(status)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .fromDate(fromDate)
                .toDate(toDate)
                .search(search)
                .build();

        byte[] csv = orderService.exportOrdersCsv(filter);

        String filename = "divyagems-orders-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ─── GET /admin/orders/{orderId} ──────────────────────────
    @Operation(summary = "Get full order detail by order ID (admin — no ownership check)")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.adminGetOrderById(orderId));
    }

    // ─── PATCH /admin/orders/{orderId}/status ─────────────────
    @Operation(summary = "Update order status (and optionally set tracking info)")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    // ─── POST /admin/orders/{orderId}/cancel ──────────────────
    @Operation(summary = "Cancel an order (admin — no status restriction)")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : "Cancelled by admin";
        return ResponseEntity.ok(orderService.adminCancelOrder(orderId, reason));
    }
}
