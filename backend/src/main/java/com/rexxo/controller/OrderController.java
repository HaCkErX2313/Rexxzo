package com.rexxo.controller;

import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import com.rexxo.service.AnalyticsService;
import com.rexxo.service.NotificationService;
import com.rexxo.service.PincodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final RefundRepository refundRepository;
    private final PincodeService pincodeService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OrderDetailDto>> myOrders(Authentication auth,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Long userId = getUser(auth).getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toDetailDto));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDetailDto> getOrder(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found"));
        return ResponseEntity.ok(toDetailDto(order));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<OrderDetailDto> placeOrder(Authentication auth,
                                                      @Valid @RequestBody PlaceOrderRequest request) {
        User user = getUser(auth);

        // Server-side address validation: Verify Indian pincode and match with State
        pincodeService.validatePincodeAndState(request.shippingPincode(), request.shippingState());

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
            .orElseThrow(() -> RexxoException.notFound("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw RexxoException.badRequest("Cart is empty");
        }

        // Validate stock and compute subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            if (p.getStock() < ci.getQuantity()) {
                throw RexxoException.badRequest("Insufficient stock for: " + p.getName());
            }
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("999")) >= 0
            ? BigDecimal.ZERO : new BigDecimal("49");

        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(request.couponCode())
                .orElseThrow(() -> RexxoException.badRequest("Invalid or expired coupon"));
            if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
                throw RexxoException.badRequest("Coupon has expired");
            }
            if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
                throw RexxoException.badRequest("Order minimum ₹" + coupon.getMinOrderAmount() + " required for this coupon");
            }
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw RexxoException.badRequest("Coupon usage limit reached");
            }
            if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
                discountAmount = subtotal.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"));
                if (coupon.getMaxDiscountAmount() != null) {
                    discountAmount = discountAmount.min(coupon.getMaxDiscountAmount());
                }
            } else {
                discountAmount = coupon.getDiscountValue();
            }
            couponCode = coupon.getCode();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discountAmount);

        String orderNumber = "RXO-" + System.currentTimeMillis() + "-" +
            UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .user(user)
            .status(Order.OrderStatus.PENDING)
            .subtotal(subtotal)
            .shippingFee(shippingFee)
            .discountAmount(discountAmount)
            .total(total)
            .couponCode(couponCode)
            .shippingName(request.shippingName())
            .shippingPhone(request.shippingPhone())
            .shippingAddress(request.shippingAddress())
            .shippingCity(request.shippingCity())
            .shippingState(request.shippingState())
            .shippingPincode(request.shippingPincode())
            .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : "PREPAID")
            .build();

        // Add items and decrement stock
        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            String img = p.getImages().stream().filter(ProductImage::getIsPrimary)
                .map(ProductImage::getImageUrl).findFirst().orElse(null);

            OrderItem oi = OrderItem.builder()
                .order(order)
                .product(p)
                .productName(p.getName())
                .productImageUrl(img)
                .unitPrice(p.getPrice())
                .quantity(ci.getQuantity())
                .subtotal(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .build();
            order.getItems().add(oi);

            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);
        }

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByCartId(cart.getId());

        // Notifications and Analytics
        notificationService.notifyOrderConfirmed(saved);
        analyticsService.trackEvent(AnalyticsEvent.EventType.ORDER_CREATED, user.getId(), null,
            "Order", saved.getId(), "total=" + saved.getTotal());

        return ResponseEntity.ok(toDetailDto(saved));
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<Map<String, String>> cancelOrder(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        if (order.getStatus() == Order.OrderStatus.DELIVERED
            || order.getStatus() == Order.OrderStatus.CANCELLED
            || order.getStatus() == Order.OrderStatus.SHIPPED
            || order.getStatus() == Order.OrderStatus.OUT_FOR_DELIVERY) {
            throw RexxoException.badRequest("This order cannot be cancelled as it is already " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Restock inventory
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }

        // If order was already paid, create a pending refund
        if (order.getPayment() != null && order.getPayment().getStatus() == Payment.PaymentStatus.PAID) {
            Refund refund = Refund.builder()
                .refundId("REF-" + System.currentTimeMillis())
                .order(order)
                .payment(order.getPayment())
                .amount(order.getTotal())
                .status(Refund.RefundStatus.REFUND_PENDING)
                .reason("Customer cancelled order #" + order.getOrderNumber())
                .provider("RAZORPAY")
                .build();
            refundRepository.save(refund);
        }

        return ResponseEntity.ok(Map.of("message", "Order successfully cancelled and items restocked"));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    private OrderDetailDto toDetailDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream().map(i ->
            new OrderItemDto(i.getId(), i.getProduct().getId(), i.getProductName(), i.getProductImageUrl(),
                i.getUnitPrice(), i.getQuantity(), i.getSubtotal())
        ).toList();

        boolean delivered = o.getStatus() == Order.OrderStatus.DELIVERED;
        LocalDateTime deliveredTime = o.getUpdatedAt() != null ? o.getUpdatedAt() : o.getCreatedAt();
        long daysSinceDelivery = ChronoUnit.DAYS.between(deliveredTime, LocalDateTime.now());
        boolean isReturnEligible = delivered && (daysSinceDelivery <= 7);

        List<ReturnRequest> returns = returnRequestRepository.findByOrderId(o.getId());
        String returnStatus = !returns.isEmpty() ? returns.get(0).getStatus().name() : null;

        String courier = o.getShipment() != null ? o.getShipment().getCourierName() : null;
        String awb = o.getShipment() != null ? o.getShipment().getAwbNumber() : null;
        String shipmentStatus = o.getShipment() != null && o.getShipment().getShipmentStatus() != null
            ? o.getShipment().getShipmentStatus().name() : null;
        String paymentStatus = o.getPayment() != null && o.getPayment().getStatus() != null
            ? o.getPayment().getStatus().name() : (o.getPaymentMethod() != null ? o.getPaymentMethod() : "PENDING");

        return new OrderDetailDto(
            o.getId(), o.getOrderNumber(), o.getStatus().name(),
            paymentStatus, o.getPaymentMethod(), courier, awb, shipmentStatus,
            isReturnEligible, returnStatus,
            o.getSubtotal(), o.getShippingFee(), o.getDiscountAmount(), o.getTotal(),
            o.getCouponCode(), o.getShippingName(), o.getShippingPhone(),
            o.getShippingAddress(), o.getShippingCity(), o.getShippingState(),
            o.getShippingPincode(), items, o.getCreatedAt()
        );
    }

    public record OrderDetailDto(
        Long id, String orderNumber, String status,
        String paymentStatus, String paymentMethod, String courier, String awb, String shipmentStatus,
        boolean isReturnEligible, String returnStatus,
        BigDecimal subtotal, BigDecimal shippingFee,
        BigDecimal discountAmount, BigDecimal total,
        String couponCode, String shippingName, String shippingPhone,
        String shippingAddress, String shippingCity, String shippingState,
        String shippingPincode, List<OrderItemDto> items,
        LocalDateTime createdAt
    ) {}

    public record OrderItemDto(
        Long id, Long productId, String productName, String imageUrl,
        BigDecimal unitPrice, Integer quantity, BigDecimal subtotal
    ) {}

    public record PlaceOrderRequest(
        @NotBlank String shippingName,
        @NotBlank String shippingPhone,
        @NotBlank String shippingAddress,
        @NotBlank String shippingCity,
        @NotBlank String shippingState,
        @NotBlank String shippingPincode,
        String couponCode,
        String paymentMethod
    ) {}
}
