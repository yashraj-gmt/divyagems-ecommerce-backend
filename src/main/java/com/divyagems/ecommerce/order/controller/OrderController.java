package com.divyagems.ecommerce.order.controller;

import com.divyagems.ecommerce.order.dto.request.PaymentVerificationRequest;
import com.divyagems.ecommerce.order.dto.request.PlaceOrderRequest;
import com.divyagems.ecommerce.order.dto.response.OrderResponse;
import com.divyagems.ecommerce.order.dto.response.OrderSummaryResponse;
import com.divyagems.ecommerce.order.dto.response.OrderTrackingResponse;
import com.divyagems.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Customer-facing order endpoints.
 * All endpoints (except track) require a valid JWT. 
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer order management — place, pay, view, track, and cancel orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    // ─── POST /orders ─────────────────────────────────────────
    @Operation(summary = "Place a new order from the active cart")
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── POST /orders/verify-payment ─────────────────────────
    @Operation(
            summary = "Verify Razorpay payment",
            description = "Dev mode: directly marks payment as PAID. Production: verifies HMAC-SHA256 signature."
    )
    @PostMapping("/verify-payment")
    public ResponseEntity<OrderResponse> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        return ResponseEntity.ok(orderService.verifyPayment(request));
    }

    // ─── GET /orders ──────────────────────────────────────────
    @Operation(summary = "Get current user's order history (paginated)")
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    // ─── GET /orders/{orderId} ────────────────────────────────
    @Operation(summary = "Get full order detail by order ID (user must own the order)")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable @Parameter(description = "Human-readable order ID e.g. DG-202601-00001") String orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    // ─── GET /orders/{orderId}/track
    @Operation(
            summary = "Public order tracking — no authentication required",
            description = "Returns order status timeline. Requires the shipping phone number for verification."
    )
    @GetMapping("/{orderId}/track")
    public ResponseEntity<OrderTrackingResponse> trackOrder(
            @PathVariable String orderId,
            @RequestParam @Parameter(description = "Shipping phone number for identity verification") String phone) {
        return ResponseEntity.ok(orderService.trackOrder(orderId, phone));
    }

    // ─── POST /orders/{orderId}/cancel ────────────────────────
    @Operation(summary = "Cancel an order (only PENDING or CONFIRMED orders can be cancelled)")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(orderService.cancelOrder(orderId, reason));
    }
}
