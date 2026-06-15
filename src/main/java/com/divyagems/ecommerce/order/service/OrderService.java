package com.divyagems.ecommerce.order.service;

import com.divyagems.ecommerce.order.dto.request.*;
import com.divyagems.ecommerce.order.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Core order service contract covering the full checkout lifecycle:
 * place → pay → track → (admin) manage → cancel / refund.
 */
public interface OrderService {

    // ─── Customer endpoints ───────────────────────────────

    /**
     * Place a new order from the authenticated user's active cart.
     * For ONLINE payments: creates a Razorpay order and returns razorpayOrderId + key.
     * For COD: immediately sets status to CONFIRMED.
     *
     * @param request checkout parameters (addresses, coupon, payment method)
     * @return full order response
     */
    OrderResponse placeOrder(PlaceOrderRequest request);

    /**
     * Verify and record a Razorpay payment after the frontend checkout widget returns.
     * In production mode: verifies HMAC-SHA256 signature.
     * Dev/stub mode: directly marks payment as PAID without signature check.
     *
     * @param request Razorpay payment identifiers + signature
     * @return updated order response with CONFIRMED status
     */
    OrderResponse verifyPayment(PaymentVerificationRequest request);

    /**
     * Return a paginated list of the current user's orders, newest first.
     *
     * @param pageable pagination and sort parameters
     * @return page of lightweight order summaries
     */
    Page<OrderSummaryResponse> getMyOrders(Pageable pageable);

    /**
     * Return full order detail for the current user.
     * Throws 403 if the order belongs to a different user.
     *
     * @param orderId human-readable order reference (e.g. DG-202601-00001)
     * @return full order response
     */
    OrderResponse getOrderById(String orderId);

    /**
     * Public order tracking — returns status timeline and shipping info.
     * Requires the order's shipping phone number for verification.
     *
     * @param orderId human-readable order ID
     * @param phone   phone number to verify against the shipping address
     * @return tracking response
     */
    OrderTrackingResponse trackOrder(String orderId, String phone);

    /**
     * Cancel an order. Only cancellable if status is PENDING or CONFIRMED.
     * Restores stock for all items. If payment was made, sets REFUND_INITIATED.
     *
     * @param orderId human-readable order ID
     * @param reason  cancellation reason stored in status history
     * @return updated order response
     */
    OrderResponse cancelOrder(String orderId, String reason);

    // ─── Admin endpoints ──────────────────────────────────

    /**
     * Return a paginated, filtered list of all orders (admin view).
     *
     * @param filter   optional filter parameters (status, date range, search)
     * @param pageable pagination and sort parameters
     * @return page of lightweight order summaries
     */
    Page<OrderSummaryResponse> getAllOrders(OrderFilterRequest filter, Pageable pageable);

    /**
     * Admin version of getOrderById — no ownership check.
     *
     * @param orderId human-readable order ID
     * @return full order response
     */
    OrderResponse adminGetOrderById(String orderId);

    /**
     * Move an order to a new status and optionally set tracking info.
     * Sends a status update email to the customer.
     *
     * @param orderId human-readable order ID
     * @param request new status, comment, and optional tracking data
     * @return updated order response
     */
    OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request);

    /**
     * Admin-triggered order cancellation — no status restriction check.
     * Restores stock and initiates refund if payment was received.
     *
     * @param orderId human-readable order ID
     * @param reason  cancellation reason
     * @return updated order response
     */
    OrderResponse adminCancelOrder(String orderId, String reason);

    /**
     * Export all (filtered) orders as a CSV byte array for download.
     *
     * @param filter optional filter parameters
     * @return raw CSV bytes
     */
    byte[] exportOrdersCsv(OrderFilterRequest filter);
}
