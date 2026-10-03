package com.rexxo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user", columnList = "user_id"),
    @Index(name = "idx_orders_number", columnList = "order_number", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Shipment shipment;

    /** Denormalized payment method for quick display (PREPAID / COD) */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // Shipping address snapshot (denormalized for order history integrity)
    @Column(name = "shipping_name", length = 100)
    private String shippingName;
    @Column(name = "shipping_phone", length = 15)
    private String shippingPhone;
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;
    @Column(name = "shipping_state", length = 100)
    private String shippingState;
    @Column(name = "shipping_pincode", length = 10)
    private String shippingPincode;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        /** Order created, awaiting payment */
        PENDING_PAYMENT,
        /** Legacy alias kept for compatibility */
        PENDING,
        /** Payment received (Razorpay verified) or COD confirmed */
        PAID,
        /** Order confirmed, awaiting processing */
        CONFIRMED,
        /** Being picked / packed */
        PROCESSING,
        /** Packed, awaiting pickup */
        PACKED,
        /** Pickup scheduled with courier */
        PICKUP_SCHEDULED,
        /** Picked up by courier */
        PICKED_UP,
        /** Shipped / in transit */
        SHIPPED,
        /** In transit */
        IN_TRANSIT,
        /** Out for delivery */
        OUT_FOR_DELIVERY,
        /** Successfully delivered */
        DELIVERED,
        /** Cancelled */
        CANCELLED,
        /** Refunded */
        REFUNDED,
        /** Return requested by customer */
        RETURN_REQUESTED,
        /** Return in transit */
        RETURN_IN_TRANSIT,
        /** Return to origin */
        RTO,
        /** Returned to seller */
        RETURNED
    }
}
