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
@Table(name = "return_requests", indexes = {
    @Index(name = "idx_returns_order", columnList = "order_id"),
    @Index(name = "idx_returns_user", columnList = "user_id"),
    @Index(name = "idx_returns_number", columnList = "return_number", unique = true),
    @Index(name = "idx_returns_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_number", nullable = false, unique = true, length = 35)
    private String returnNumber;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "items", "user", "payment", "shipment"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "addresses", "cart"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReturnItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.RETURN_REQUESTED;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "pickup_awb", length = 50)
    private String pickupAwb;

    @Column(name = "pickup_courier", length = 50)
    private String pickupCourier;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "order", "payment"})
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "refund_id")
    private Refund refund;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReturnStatus {
        RETURN_REQUESTED,
        RETURN_APPROVED,
        RETURN_REJECTED,
        PICKUP_SCHEDULED,
        PICKED_UP,
        RECEIVED,
        QUALITY_CHECK,
        REFUND_PENDING,
        REFUNDED,
        RETURN_CANCELLED
    }
}
