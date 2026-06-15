package com.divyagems.ecommerce.order.service.impl;

import com.divyagems.ecommerce.cart.repository.CartRepository;
import com.divyagems.ecommerce.email.EmailService;
import com.divyagems.ecommerce.entity.*;
import com.divyagems.ecommerce.enums.*;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.order.dto.request.*;
import com.divyagems.ecommerce.order.dto.response.*;
import com.divyagems.ecommerce.order.repository.*;
import com.divyagems.ecommerce.order.service.OrderIdGenerator;
import com.divyagems.ecommerce.order.service.OrderService;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.product.repository.ProductVariantRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // ─── Repositories ──────────────────────────────────────────
    private final OrderRepository              orderRepository;
    private final OrderItemRepository          orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository            paymentRepository;
    private final CouponRepository             couponRepository;
    private final AddressRepository            addressRepository;
    private final CartRepository               cartRepository;
    private final UserRepository               userRepository;
    private final ProductRepository            productRepository;
    private final ProductVariantRepository     variantRepository;

    // ─── Services ──────────────────────────────────────────────
    private final OrderIdGenerator orderIdGenerator;
    private final EmailService     emailService;
    private final RazorpayClient   razorpayClient;

    // ─── Config ────────────────────────────────────────────────
    @Value("${app.order.free-shipping-threshold:999}")
    private BigDecimal freeShippingThreshold;

    @Value("${app.order.shipping-charge:99}")
    private BigDecimal shippingCharge;

    @Value("${app.order.tax-rate:0.00}")
    private BigDecimal taxRate;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${app.razorpay.currency:INR}")
    private String razorpayCurrency;

    // ═══════════════════════════════════════════════════════════
    // 1. PLACE ORDER
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = requireCurrentUser();

        // ── Step 1: Get active cart ────────────────────────────
        Cart cart = cartRepository.findByUserAndIsActiveTrue(user)
                .orElseThrow(() -> new BusinessException("Your cart is empty. Please add items before checking out."));

        if (cart.getCartItems().isEmpty()) {
            throw new BusinessException("Your cart is empty. Please add items before checking out.");
        }

        // ── Step 2: Validate cart items (stock + active status) ─
        validateCartItems(cart);

        // ── Step 3: Resolve addresses ──────────────────────────
        OrderAddress shippingAddr = resolveShippingAddress(request, user);
        OrderAddress billingAddr  = resolveBillingAddress(request, user, shippingAddr);

        // ── Step 4: Calculate financials ───────────────────────
        BigDecimal subtotal = computeSubtotal(cart);
        BigDecimal shipping = subtotal.compareTo(freeShippingThreshold) >= 0
                ? BigDecimal.ZERO
                : shippingCharge;

        // ── Step 5: Apply coupon ───────────────────────────────
        Coupon coupon        = null;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        String couponCode    = null;

        if (StringUtils.hasText(request.getCouponCode())) {
            coupon        = validateAndApplyCoupon(request.getCouponCode().trim().toUpperCase(), subtotal, user);
            couponDiscount = calculateCouponDiscount(coupon, subtotal);
            couponCode    = coupon.getCode();
        }

        BigDecimal discountAmount = couponDiscount; // extend for future product discounts
        BigDecimal taxableAmount  = subtotal.subtract(discountAmount).add(shipping);
        BigDecimal taxAmount      = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount    = taxableAmount.add(taxAmount);

        // ── Step 6: Generate order ID ──────────────────────────
        String orderId = orderIdGenerator.generateNextOrderId();

        // ── Step 7: Build and save order ──────────────────────
        Order order = Order.builder()
                .orderId(orderId)
                .user(user)
                .orderStatus(OrderStatusEnum.PENDING)
                .paymentStatus(PaymentStatusEnum.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .shippingCharge(shipping)
                .discountAmount(discountAmount)
                .couponCode(couponCode)
                .couponDiscount(couponDiscount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .shippingAddress(shippingAddr)
                .billingAddress(billingAddr)
                .notes(request.getNotes())
                .build();

        // ── Step 8: Snapshot order items ──────────────────────
        List<OrderItem> orderItems = buildOrderItems(cart, order);
        order.getOrderItems().addAll(orderItems);

        // ── Step 9: Handle payment method ─────────────────────
        if (request.getPaymentMethod() == PaymentMethodEnum.ONLINE) {
            String razorpayOrderId = createRazorpayOrder(orderId, totalAmount);
            order.setRazorpayOrderId(razorpayOrderId);
        } else {
            // COD: immediately confirmed
            order.setOrderStatus(OrderStatusEnum.CONFIRMED);
        }

        orderRepository.save(order);

        // ── Step 10: Deduct stock ──────────────────────────────
        deductStock(cart);

        // ── Step 11: Increment coupon usage ───────────────────
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        // ── Step 12: Clear cart ────────────────────────────────
        cart.getCartItems().clear();
        cart.setActive(false);
        cartRepository.save(cart);

        // ── Step 13: Save initial status history ──────────────
        OrderStatusEnum initialStatus = order.getOrderStatus(); // PENDING or CONFIRMED
        appendStatusHistory(order, initialStatus, "Order placed successfully", "SYSTEM");

        orderRepository.save(order);

        // ── Step 14: Send confirmation email (async) ──────────
        sendOrderConfirmationEmailAsync(user.getEmail(), orderId, totalAmount);

        log.info("Order placed: {} | user: {} | payment: {} | total: {}",
                orderId, user.getEmail(), request.getPaymentMethod(), totalAmount);

        return buildOrderResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // 2. VERIFY PAYMENT
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse verifyPayment(PaymentVerificationRequest request) {
        Order order = orderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", request.getOrderId()));

        if (order.getPaymentStatus() == PaymentStatusEnum.PAID) {
            log.warn("Payment already verified for order: {}", request.getOrderId());
            return buildOrderResponse(order);
        }

        // ── Signature verification (dev: accept any; prod: validate HMAC) ──
        boolean isValid = verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (isValid) {
            order.setPaymentStatus(PaymentStatusEnum.PAID);
            order.setOrderStatus(OrderStatusEnum.CONFIRMED);
            order.setRazorpayPaymentId(request.getRazorpayPaymentId());
            order.setRazorpaySignature(request.getRazorpaySignature());

            // Record Payment entity
            Payment payment = Payment.builder()
                    .order(order)
                    .transactionId(request.getRazorpayPaymentId())
                    .gatewayName(GatewayNameEnum.RAZORPAY)
                    .amount(order.getTotalAmount())
                    .currency(razorpayCurrency)
                    .status(PaymentStatusEnum.PAID)
                    .gatewayResponse(buildGatewayResponseJson(request))
                    .paidAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            appendStatusHistory(order, OrderStatusEnum.CONFIRMED, "Payment verified successfully", "SYSTEM");
            sendPaymentConfirmationEmailAsync(order.getUser().getEmail(), order.getOrderId());
            log.info("Payment verified for order: {}", order.getOrderId());
        } else {
            order.setPaymentStatus(PaymentStatusEnum.FAILED);
            appendStatusHistory(order, order.getOrderStatus(), "Payment verification failed — invalid signature", "SYSTEM");
            log.warn("Payment verification FAILED for order: {}", order.getOrderId());
        }

        orderRepository.save(order);
        return buildOrderResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // 3. GET MY ORDERS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Pageable pageable) {
        User user = requireCurrentUser();
        return orderRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::buildOrderSummaryResponse);
    }

    // ═══════════════════════════════════════════════════════════
    // 4. GET ORDER BY ID (user-scoped)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId) {
        User user = requireCurrentUser();
        Order order = orderRepository.findByOrderIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return buildOrderResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // 5. TRACK ORDER (public)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse trackOrder(String orderId, String phone) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        // Phone verification — compare against shipping address
        String orderPhone = order.getShippingAddress() != null
                ? order.getShippingAddress().getPhone()
                : null;

        if (!StringUtils.hasText(phone) || !phone.equals(orderPhone)) {
            throw new BusinessException("Phone number does not match the order records.");
        }

        List<OrderStatusHistoryResponse> history = statusHistoryRepository
                .findByOrderOrderByChangedAtAsc(order)
                .stream()
                .map(this::mapStatusHistory)
                .collect(Collectors.toList());

        return OrderTrackingResponse.builder()
                .orderId(order.getOrderId())
                .currentStatus(order.getOrderStatus())
                .statusHistory(history)
                .trackingNumber(order.getTrackingNumber())
                .shippingProvider(order.getShippingProvider())
                .estimatedDelivery(order.getEstimatedDelivery())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 6. CANCEL ORDER (user or admin)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        User user = requireCurrentUser();
        Order order = orderRepository.findByOrderIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return performCancellation(order, reason, user.getEmail());
    }

    // ═══════════════════════════════════════════════════════════
    // 7. ADMIN — GET ALL ORDERS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(OrderFilterRequest filter, Pageable pageable) {
        Specification<Order> spec = buildOrderSpec(filter);
        return orderRepository.findAll(spec, pageable)
                .map(this::buildOrderSummaryResponse);
    }

    // ═══════════════════════════════════════════════════════════
    // 8. ADMIN — GET ORDER BY ID (no ownership check)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public OrderResponse adminGetOrderById(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return buildOrderResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // 9. ADMIN — UPDATE ORDER STATUS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        String actor = getCurrentUserEmail();
        order.setOrderStatus(request.getStatus());

        if (StringUtils.hasText(request.getTrackingNumber())) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        if (StringUtils.hasText(request.getShippingProvider())) {
            order.setShippingProvider(request.getShippingProvider());
        }
        if (request.getStatus() == OrderStatusEnum.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        appendStatusHistory(order, request.getStatus(), request.getComment(), actor);
        orderRepository.save(order);

        // Async status update email
        sendStatusUpdateEmailAsync(order.getUser().getEmail(), orderId, request.getStatus().name());

        log.info("Order {} status updated to {} by {}", orderId, request.getStatus(), actor);
        return buildOrderResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // 10. ADMIN — CANCEL ORDER
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse adminCancelOrder(String orderId, String reason) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        String actor = getCurrentUserEmail();
        return performCancellation(order, reason, actor);
    }

    // ═══════════════════════════════════════════════════════════
    // 11. ADMIN — EXPORT CSV
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersCsv(OrderFilterRequest filter) {
        Specification<Order> spec = buildOrderSpec(filter);
        List<Order> orders = orderRepository.findAll(spec);

        StringBuilder csv = new StringBuilder();
        csv.append("Order ID,Status,Payment Status,Payment Method,Customer Email,Total Amount,Coupon Code,Tracking Number,Created At\n");

        for (Order o : orders) {
            csv.append(escape(o.getOrderId())).append(",")
               .append(o.getOrderStatus()).append(",")
               .append(o.getPaymentStatus()).append(",")
               .append(o.getPaymentMethod()).append(",")
               .append(escape(o.getUser().getEmail())).append(",")
               .append(o.getTotalAmount()).append(",")
               .append(o.getCouponCode() != null ? escape(o.getCouponCode()) : "").append(",")
               .append(o.getTrackingNumber() != null ? escape(o.getTrackingNumber()) : "").append(",")
               .append(o.getCreatedAt()).append("\n");
        }

        log.info("Exported {} orders to CSV", orders.size());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Cart Validation
    // ═══════════════════════════════════════════════════════════

    private void validateCartItems(Cart cart) {
        List<String> errors = new ArrayList<>();

        for (CartItem item : cart.getCartItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            if (product.getStatus() != ProductStatusEnum.ACTIVE) {
                errors.add("'" + product.getName() + "' is no longer available.");
                continue;
            }
            if (variant != null && !variant.isActive()) {
                errors.add("A variant of '" + product.getName() + "' is no longer available.");
                continue;
            }

            int stock = variant != null ? variant.getStockQuantity() : product.getStockQuantity();
            if (item.getQuantity() > stock) {
                errors.add("Insufficient stock for '" + product.getName()
                        + "'. Requested: " + item.getQuantity() + ", Available: " + stock);
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessException("Cart validation failed: " + String.join("; ", errors));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Address Resolution
    // ═══════════════════════════════════════════════════════════

    private OrderAddress resolveShippingAddress(PlaceOrderRequest request, User user) {
        if (request.getShippingAddressId() != null) {
            Address addr = addressRepository.findByIdAndUser(request.getShippingAddressId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Shipping address", "id", request.getShippingAddressId()));
            return mapToOrderAddress(addr);
        }
        if (request.getNewShippingAddress() != null) {
            return mapToOrderAddress(request.getNewShippingAddress());
        }
        throw new BusinessException("Please provide a shipping address.");
    }

    private OrderAddress resolveBillingAddress(PlaceOrderRequest request, User user, OrderAddress shippingAddr) {
        if (request.isSameAsBilling()) {
            return shippingAddr; // copy shipping → billing
        }
        if (request.getBillingAddressId() != null) {
            Address addr = addressRepository.findByIdAndUser(request.getBillingAddressId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Billing address", "id", request.getBillingAddressId()));
            return mapToOrderAddress(addr);
        }
        if (request.getNewBillingAddress() != null) {
            return mapToOrderAddress(request.getNewBillingAddress());
        }
        throw new BusinessException("Please provide a billing address or select 'same as shipping'.");
    }

    private OrderAddress mapToOrderAddress(Address addr) {
        return OrderAddress.builder()
                .firstName(addr.getFirstName())
                .lastName(addr.getLastName())
                .phone(addr.getPhone())
                .addressLine1(addr.getAddressLine1())
                .addressLine2(addr.getAddressLine2())
                .city(addr.getCity())
                .state(addr.getState())
                .pinCode(addr.getPinCode())
                .country(addr.getCountry())
                .build();
    }

    private OrderAddress mapToOrderAddress(AddressRequest req) {
        return OrderAddress.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .state(req.getState())
                .pinCode(req.getPinCode())
                .country(req.getCountry() != null ? req.getCountry() : "India")
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Financial Calculations
    // ═══════════════════════════════════════════════════════════

    private BigDecimal computeSubtotal(Cart cart) {
        return cart.getCartItems().stream()
                .map(item -> resolveEffectivePrice(item.getProduct(), item.getVariant())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveEffectivePrice(Product product, ProductVariant variant) {
        if (variant != null && variant.getPrice() != null
                && variant.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getSalePrice() != null
                    && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                    ? variant.getSalePrice()
                    : variant.getPrice();
        }
        return product.getSalePrice() != null
                && product.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                ? product.getSalePrice()
                : product.getPrice();
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Coupon Validation
    // ═══════════════════════════════════════════════════════════

    private Coupon validateAndApplyCoupon(String code, BigDecimal subtotal, User user) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new BusinessException("Coupon '" + code + "' is invalid or inactive."));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new BusinessException("Coupon '" + code + "' has expired.");
        }
        if (subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BusinessException("This coupon requires a minimum order of ₹"
                    + coupon.getMinimumOrderAmount() + ".");
        }
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessException("Coupon '" + code + "' usage limit has been reached.");
        }
        if (coupon.isOneTimePerUser() && orderRepository.existsByUserAndCouponCode(user, code)) {
            throw new BusinessException("You have already used coupon '" + code + "'.");
        }
        return coupon;
    }

    private BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountTypeEnum.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }
        // Cap at maximumDiscountAmount if set
        if (coupon.getMaximumDiscountAmount() != null
                && discount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
            discount = coupon.getMaximumDiscountAmount();
        }
        // Discount cannot exceed subtotal
        return discount.min(subtotal);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Order Items Builder
    // ═══════════════════════════════════════════════════════════

    private List<OrderItem> buildOrderItems(Cart cart, Order order) {
        return cart.getCartItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            BigDecimal unitPrice  = resolveEffectivePrice(product, variant);
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            // Primary image URL — product level fallback
            String imageUrl = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(product.getImages().isEmpty() ? null
                            : product.getImages().get(0).getImageUrl());

            // Use variant image if available
            if (variant != null && StringUtils.hasText(variant.getImageUrl())) {
                imageUrl = variant.getImageUrl();
            }

            String variantName = variant != null ? variant.getColorName() : null;
            String sku = variant != null ? variant.getSku() : product.getSku();

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .variantName(variantName)
                    .sku(sku)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .productImageUrl(imageUrl)
                    .build();
        }).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Stock Deduction
    // ═══════════════════════════════════════════════════════════

    private void deductStock(Cart cart) {
        for (CartItem item : cart.getCartItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            if (variant != null) {
                variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
                variantRepository.save(variant);
            } else {
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            if (item.getVariant() != null) {
                ProductVariant variant = item.getVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                variantRepository.save(variant);
            } else {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Razorpay Integration
    // ═══════════════════════════════════════════════════════════

    private String createRazorpayOrder(String internalOrderId, BigDecimal totalAmount) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in paise (INR × 100)
            orderRequest.put("amount", totalAmount.multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", razorpayCurrency);
            orderRequest.put("receipt", internalOrderId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id").toString();
            log.info("Razorpay order created: {} for internal orderId: {}", razorpayOrderId, internalOrderId);
            return razorpayOrderId;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for {}: {}", internalOrderId, e.getMessage());
            throw new BusinessException("Payment gateway error. Please try again.");
        }
    }

    /**
     * Verify HMAC-SHA256 signature per Razorpay spec:
     *   signature = HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     *
     * NOTE: In dev mode (placeholder secret), signature verification is skipped.
     */
    private boolean verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        if ("placeholder_secret".equals(razorpayKeySecret)) {
            log.warn("DEV MODE: Skipping Razorpay signature verification");
            return true;
        }
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hash);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String buildGatewayResponseJson(PaymentVerificationRequest request) {
        return String.format(
                "{\"razorpayOrderId\":\"%s\",\"razorpayPaymentId\":\"%s\",\"razorpaySignature\":\"%s\"}",
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Cancellation Logic
    // ═══════════════════════════════════════════════════════════

    private OrderResponse performCancellation(Order order, String reason, String actor) {
        OrderStatusEnum currentStatus = order.getOrderStatus();

        if (currentStatus != OrderStatusEnum.PENDING && currentStatus != OrderStatusEnum.CONFIRMED) {
            throw new BusinessException(
                    "Order cannot be cancelled. Current status: " + currentStatus
                    + ". Only PENDING or CONFIRMED orders can be cancelled.");
        }

        order.setOrderStatus(OrderStatusEnum.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        // Restore stock
        restoreStock(order);

        // If paid: initiate refund
        if (order.getPaymentStatus() == PaymentStatusEnum.PAID) {
            order.setOrderStatus(OrderStatusEnum.REFUND_INITIATED);
            appendStatusHistory(order, OrderStatusEnum.REFUND_INITIATED,
                    "Refund initiated: " + (reason != null ? reason : "Customer requested cancellation"), actor);
        } else {
            appendStatusHistory(order, OrderStatusEnum.CANCELLED,
                    reason != null ? reason : "Customer requested cancellation", actor);
        }

        orderRepository.save(order);

        sendStatusUpdateEmailAsync(order.getUser().getEmail(), order.getOrderId(),
                order.getOrderStatus().name());

        log.info("Order {} cancelled by {}. Reason: {}", order.getOrderId(), actor, reason);
        return buildOrderResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Status History
    // ═══════════════════════════════════════════════════════════

    private void appendStatusHistory(Order order, OrderStatusEnum status, String comment, String changedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .comment(comment)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
        order.getStatusHistory().add(history);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Specification (Admin filter)
    // ═══════════════════════════════════════════════════════════

    private Specification<Order> buildOrderSpec(OrderFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("orderStatus"), filter.getStatus()));
                }
                if (filter.getPaymentStatus() != null) {
                    predicates.add(cb.equal(root.get("paymentStatus"), filter.getPaymentStatus()));
                }
                if (filter.getPaymentMethod() != null) {
                    predicates.add(cb.equal(root.get("paymentMethod"), filter.getPaymentMethod()));
                }
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"), filter.getFromDate().atStartOfDay()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
                }
                if (StringUtils.hasText(filter.getSearch())) {
                    String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("orderId")), pattern),
                            cb.like(cb.lower(root.get("user").get("email")), pattern)
                    ));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Response Builders
    // ═══════════════════════════════════════════════════════════

    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::mapOrderItem)
                .collect(Collectors.toList());

        List<OrderStatusHistoryResponse> history = statusHistoryRepository
                .findByOrderOrderByChangedAtAsc(order)
                .stream()
                .map(this::mapStatusHistory)
                .collect(Collectors.toList());

        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .items(items)
                .shippingAddress(mapOrderAddress(order.getShippingAddress()))
                .billingAddress(mapOrderAddress(order.getBillingAddress()))
                .subtotal(order.getSubtotal())
                .shippingCharge(order.getShippingCharge())
                .discountAmount(order.getDiscountAmount())
                .couponCode(order.getCouponCode())
                .couponDiscount(order.getCouponDiscount())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .trackingNumber(order.getTrackingNumber())
                .shippingProvider(order.getShippingProvider())
                .estimatedDelivery(order.getEstimatedDelivery())
                .statusHistory(history)
                .createdAt(order.getCreatedAt());

        // Expose Razorpay fields only for ONLINE pending payments
        if (order.getPaymentMethod() == PaymentMethodEnum.ONLINE
                && order.getPaymentStatus() == PaymentStatusEnum.PENDING) {
            builder.razorpayOrderId(order.getRazorpayOrderId())
                   .razorpayKeyId(razorpayKeyId);
        }

        return builder.build();
    }

    private OrderSummaryResponse buildOrderSummaryResponse(Order order) {
        int itemCount = order.getOrderItems().size();

        String primaryImageUrl = order.getOrderItems().stream()
                .findFirst()
                .map(OrderItem::getProductImageUrl)
                .orElse(null);

        return OrderSummaryResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(itemCount)
                .primaryImageUrl(primaryImageUrl)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse mapOrderItem(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .productImageUrl(item.getProductImageUrl())
                .build();
    }

    private OrderStatusHistoryResponse mapStatusHistory(OrderStatusHistory h) {
        return OrderStatusHistoryResponse.builder()
                .status(h.getStatus())
                .comment(h.getComment())
                .changedBy(h.getChangedBy())
                .changedAt(h.getChangedAt())
                .build();
    }

    private OrderAddressResponse mapOrderAddress(OrderAddress addr) {
        if (addr == null) return null;
        return OrderAddressResponse.builder()
                .firstName(addr.getFirstName())
                .lastName(addr.getLastName())
                .phone(addr.getPhone())
                .addressLine1(addr.getAddressLine1())
                .addressLine2(addr.getAddressLine2())
                .city(addr.getCity())
                .state(addr.getState())
                .pinCode(addr.getPinCode())
                .country(addr.getCountry())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Auth Helpers
    // ═══════════════════════════════════════════════════════════

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", auth.getName()));
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — CSV Helper
    // ═══════════════════════════════════════════════════════════

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Async Email Dispatchers
    // ═══════════════════════════════════════════════════════════

    @Async
    protected void sendOrderConfirmationEmailAsync(String email, String orderId, BigDecimal total) {
        try {
            emailService.sendOrderConfirmationEmail(email, orderId, total);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for {}: {}", orderId, e.getMessage());
        }
    }

    @Async
    protected void sendPaymentConfirmationEmailAsync(String email, String orderId) {
        try {
            emailService.sendPaymentConfirmationEmail(email, orderId);
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email for {}: {}", orderId, e.getMessage());
        }
    }

    @Async
    protected void sendStatusUpdateEmailAsync(String email, String orderId, String status) {
        try {
            emailService.sendOrderStatusUpdateEmail(email, orderId, status);
        } catch (Exception e) {
            log.error("Failed to send status update email for {}: {}", orderId, e.getMessage());
        }
    }
}
