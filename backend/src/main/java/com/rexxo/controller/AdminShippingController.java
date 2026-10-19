package com.rexxo.controller;

import com.rexxo.config.ShiprocketConfig;
import com.rexxo.entity.Order;
import com.rexxo.entity.Shipment;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.OrderRepository;
import com.rexxo.repository.ShipmentRepository;
import com.rexxo.shipping.ShippingDtos;
import com.rexxo.shipping.ShiprocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Admin-only endpoints for Shiprocket management.
 * All endpoints require ADMIN role via Spring Security.
 */
@RestController
@RequestMapping("/api/admin/shipping")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminShippingController {

    private final ShiprocketService shiprocketService;
    private final ShiprocketConfig shiprocketConfig;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    // ─── Shiprocket Health/Status ────────────────────────────────────────────

    /**
     * GET /api/admin/shipping/status
     * Tests Shiprocket connectivity (authentication). Never returns token or credentials.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> shiprocketStatus() {
        Map<String, Object> status = shiprocketService.testAuthentication();
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/admin/shipping/pickup-locations
     * Lists pickup locations configured in Shiprocket for verification.
     */
    @GetMapping("/pickup-locations")
    public ResponseEntity<?> getPickupLocations() {
        com.fasterxml.jackson.databind.JsonNode locations = shiprocketService.getPickupLocations();
        return ResponseEntity.ok(Map.of(
            "configuredLocation", shiprocketConfig.getPickupLocation(),
            "pickupPincode", shiprocketConfig.getPickupPincode(),
            "locations", locations != null ? locations : Map.of()
        ));
    }

    /**
     * POST /api/admin/shipping/shipments/{shipmentId}/assign-courier
     * Assigns courier and generates AWB for an existing shipment.
     */
    @PostMapping("/shipments/{shipmentId}/assign-courier")
    public ResponseEntity<ShippingDtos.CreateShipmentResponse> assignCourier(
            @PathVariable Long shipmentId,
            @RequestBody Map<String, Integer> body) {
        Integer courierId = body.get("courierId");
        ShippingDtos.CreateShipmentResponse response = shiprocketService.assignCourier(shipmentId, courierId);
        return ResponseEntity.ok(response);
    }

    // ─── Create / Retry Shipment ─────────────────────────────────────────────

    /**
     * POST /api/admin/shipping/orders/{orderId}/create-shipment
     * Admin triggers shipment creation for an order. Safe to retry on failure.
     */
    @PostMapping("/orders/{orderId}/create-shipment")
    public ResponseEntity<ShippingDtos.CreateShipmentResponse> createShipment(
            @PathVariable Long orderId,
            @RequestBody(required = false) CreateShipmentBody body) {

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> RexxoException.notFound("Order not found: " + orderId));

        String paymentMode = body != null ? body.paymentMode() : null;
        if (paymentMode == null || paymentMode.isBlank()) {
            paymentMode = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? "COD" : "PREPAID";
        }
        BigDecimal codAmount = (body != null && body.codAmount() != null)
            ? body.codAmount()
            : ("COD".equalsIgnoreCase(paymentMode) ? order.getTotal() : BigDecimal.ZERO);
        Integer courierId = body != null ? body.courierId() : null;

        ShippingDtos.CreateShipmentRequest req = ShippingDtos.CreateShipmentRequest.builder()
            .orderId(orderId)
            .paymentMode(paymentMode)
            .courierId(courierId)
            .codAmount(codAmount)
            .build();

        ShippingDtos.CreateShipmentResponse response = shiprocketService.createShipment(req);
        return ResponseEntity.ok(response);
    }

    // ─── Tracking (admin can view any order) ────────────────────────────────

    /**
     * GET /api/admin/shipping/orders/{orderId}/tracking
     * Admin can view tracking for any order.
     */
    @GetMapping("/orders/{orderId}/tracking")
    public ResponseEntity<ShippingDtos.TrackingResponse> getTracking(@PathVariable Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
        if (shipment == null) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> RexxoException.notFound("Order not found"));
            return ResponseEntity.ok(ShippingDtos.TrackingResponse.builder()
                .orderNumber(order.getOrderNumber())
                .shipmentStatus("No shipment created")
                .events(java.util.List.of())
                .build());
        }
        return ResponseEntity.ok(shiprocketService.getTracking(shipment.getId()));
    }

    // ─── Cancel Shipment ────────────────────────────────────────────────────

    /**
     * POST /api/admin/shipping/shipments/{shipmentId}/cancel
     * Admin cancels a shipment at Shiprocket.
     */
    @PostMapping("/shipments/{shipmentId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelShipment(@PathVariable Long shipmentId) {
        boolean success = shiprocketService.cancelShipment(shipmentId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Shipment cancelled" : "Cancellation failed"
        ));
    }

    // ─── Generate Label ──────────────────────────────────────────────────────

    /**
     * POST /api/admin/shipping/shipments/{shipmentId}/label
     * Returns label PDF URL for printing.
     */
    @PostMapping("/shipments/{shipmentId}/label")
    public ResponseEntity<Map<String, Object>> generateLabel(@PathVariable Long shipmentId) {
        String labelUrl = shiprocketService.generateLabel(shipmentId);
        if (labelUrl == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Label not available yet"));
        }
        return ResponseEntity.ok(Map.of("success", true, "labelUrl", labelUrl));
    }

    // ─── Shipment Details ────────────────────────────────────────────────────

    /**
     * GET /api/admin/shipping/orders/{orderId}/shipment
     * Returns full shipment details for admin order view.
     */
    @GetMapping("/orders/{orderId}/shipment")
    public ResponseEntity<ShipmentDetailDto> getShipmentDetail(@PathVariable Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
            .orElseThrow(() -> RexxoException.notFound("No shipment for order " + orderId));

        return ResponseEntity.ok(new ShipmentDetailDto(
            shipment.getId(),
            shipment.getOrder().getOrderNumber(),
            shipment.getProviderOrderId(),
            shipment.getProviderShipmentId(),
            shipment.getAwbNumber(),
            shipment.getCourierName(),
            shipment.getCourierId(),
            shipment.getShipmentStatus().name(),
            shipment.getPaymentMode() != null ? shipment.getPaymentMode().name() : null,
            shipment.getCodAmount(),
            shipment.getShippingCharge(),
            shipment.getTrackingUrl(),
            shipment.getPickupPincode(),
            shipment.getDeliveryPincode(),
            shipment.getAttemptCount() != null ? shipment.getAttemptCount() : 0,
            shipment.getLastAttemptAt() != null ? shipment.getLastAttemptAt().toString() : null,
            shipment.getCreatedAt() != null ? shipment.getCreatedAt().toString() : null,
            shipment.getFailureReason()
        ));
    }

    // ─── Records ─────────────────────────────────────────────────────────────

    record CreateShipmentBody(
        String paymentMode,
        Integer courierId,
        BigDecimal codAmount
    ) {}

    record ShipmentDetailDto(
        Long id,
        String orderNumber,
        String providerOrderId,
        String providerShipmentId,
        String awbNumber,
        String courierName,
        Integer courierId,
        String shipmentStatus,
        String paymentMode,
        BigDecimal codAmount,
        BigDecimal shippingCharge,
        String trackingUrl,
        String pickupPincode,
        String deliveryPincode,
        Integer attemptCount,
        String lastAttemptAt,
        String createdAt,
        String failureReason
    ) {}
}
