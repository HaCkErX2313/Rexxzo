package com.rexxo.shipping;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** DTOs for the Shipping abstraction layer. */
public final class ShippingDtos {

    private ShippingDtos() {}

    // ─── Serviceability ──────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ServiceabilityRequest {
        private String pickupPincode;
        private String deliveryPincode;
        /** Weight in grams */
        private int weightGrams;
        /** Length in cm */
        private BigDecimal lengthCm;
        /** Width in cm */
        private BigDecimal widthCm;
        /** Height in cm */
        private BigDecimal heightCm;
        private boolean cod;
        private BigDecimal orderValue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ServiceabilityResponse {
        private boolean serviceable;
        private String message;
        private List<CourierOption> couriers;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CourierOption {
        private String courierId;
        private String courierName;
        private BigDecimal rate;
        private int estimatedDeliveryDays;
        private boolean codAvailable;
        private BigDecimal codCharges;
        private String ratingComment;
    }

    // ─── Shipment Creation ───────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateShipmentRequest {
        private Long orderId;
        private String paymentMode; // "PREPAID" or "COD"
        private Integer courierId;
        private BigDecimal codAmount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateShipmentResponse {
        private boolean success;
        private String providerOrderId;
        private String providerShipmentId;
        private String awbNumber;
        private String courierName;
        private Integer courierId;
        private String trackingUrl;
        private String message;
    }

    // ─── Tracking ────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrackingResponse {
        private String orderNumber;
        private String shipmentStatus;
        private String awbNumber;
        private String courierName;
        private String estimatedDeliveryDate;
        private String trackingUrl;
        private TrackingEvent latestEvent;
        private List<TrackingEvent> events;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrackingEvent {
        private String status;
        private String rawStatus;
        private String description;
        private String location;
        private String eventTimestamp;
    }
}
