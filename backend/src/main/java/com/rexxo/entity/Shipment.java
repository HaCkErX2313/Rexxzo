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
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipments_order", columnList = "order_id"),
    @Index(name = "idx_shipments_awb", columnList = "awb_number"),
    @Index(name = "idx_shipments_provider_order", columnList = "provider_order_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** Logistics provider name, e.g. SHIPROCKET */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "SHIPROCKET";

    /** Shiprocket's order_id returned after order creation */
    @Column(name = "provider_order_id", length = 100)
    private String providerOrderId;

    /** Shiprocket's shipment_id */
    @Column(name = "provider_shipment_id", length = 100)
    private String providerShipmentId;

    /** Air Waybill tracking number */
    @Column(name = "awb_number", length = 100)
    private String awbNumber;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "courier_id")
    private Integer courierId;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false, length = 40)
    @Builder.Default
    private ShipmentStatus shipmentStatus = ShipmentStatus.CREATION_PENDING;

    /** Raw status string from Shiprocket, preserved for debugging */
    @Column(name = "raw_provider_status", length = 200)
    private String rawProviderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    private PaymentMode paymentMode;

    @Column(name = "cod_amount", precision = 10, scale = 2)
    private BigDecimal codAmount;

    @Column(name = "shipping_charge", precision = 10, scale = 2)
    private BigDecimal shippingCharge;

    /** Weight in grams */
    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "length_cm", precision = 6, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 6, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "pickup_pincode", length = 10)
    private String pickupPincode;

    @Column(name = "delivery_pincode", length = 10)
    private String deliveryPincode;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "pickup_scheduled_at")
    private LocalDateTime pickupScheduledAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventTimestamp ASC")
    @Builder.Default
    private List<ShipmentTrackingEvent> trackingEvents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ShipmentStatus {
        /** Initial state before Shiprocket order is created */
        CREATION_PENDING,
        /** Shiprocket order created but no shipment yet */
        CREATED,
        /** AWB assigned to shipment */
        AWB_ASSIGNED,
        /** Pickup scheduled */
        PICKUP_SCHEDULED,
        /** Picked up by courier */
        PICKED_UP,
        /** In transit */
        IN_TRANSIT,
        /** Out for delivery */
        OUT_FOR_DELIVERY,
        /** Successfully delivered */
        DELIVERED,
        /** Cancelled */
        CANCELLED,
        /** Shipment creation failed */
        FAILED,
        /** Return initiated */
        RETURN_INITIATED,
        /** Return in transit */
        RETURN_IN_TRANSIT,
        /** Return to origin */
        RTO,
        /** Returned successfully */
        RETURNED
    }

    public enum PaymentMode {
        PREPAID, COD
    }
}
