package com.rexxo.controller;

import com.rexxo.config.ShiprocketConfig;
import com.rexxo.entity.Order;
import com.rexxo.entity.Shipment;
import com.rexxo.entity.User;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.OrderRepository;
import com.rexxo.repository.ShipmentRepository;
import com.rexxo.repository.UserRepository;
import com.rexxo.shipping.ShippingDtos;
import com.rexxo.shipping.ShiprocketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {

    private final ShiprocketService shiprocketService;
    private final ShiprocketConfig shiprocketConfig;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final com.rexxo.service.PincodeService pincodeService;

    // ─── Pincode Lookup & Autofill ──────────────────────────────────────────

    /**
     * GET /api/shipping/pincode/{pincode}
     * Real-time Indian pincode lookup with city, district, state, and location options.
     */
    @GetMapping("/pincode/{pincode}")
    public ResponseEntity<com.rexxo.dto.shipping.PincodeLookupResponse> lookupPincode(@PathVariable String pincode) {
        com.rexxo.dto.shipping.PincodeLookupResponse response = pincodeService.lookupPincode(pincode);
        return ResponseEntity.ok(response);
    }

    // ─── Serviceability (authenticated users) ───────────────────────────────

    /**
     * POST /api/shipping/serviceability
     * Checks if delivery is available between pincodes and returns courier options.
     * Auth required — Shiprocket credentials never sent to browser.
     */
    @PostMapping("/serviceability")
    public ResponseEntity<ShippingDtos.ServiceabilityResponse> checkServiceability(
            @Valid @RequestBody ServiceabilityRequest request) {

        ShippingDtos.ServiceabilityRequest req = ShippingDtos.ServiceabilityRequest.builder()
            .pickupPincode(shiprocketConfig.getPickupPincode())
            .deliveryPincode(request.deliveryPincode())
            .weightGrams(request.weightGrams() != null ? request.weightGrams() : 500)
            .cod(Boolean.TRUE.equals(request.cod()))
            .orderValue(request.orderValue())
            .build();

        ShippingDtos.ServiceabilityResponse response = shiprocketService.checkServiceability(req);
        return ResponseEntity.ok(response);
    }

    // ─── Order Tracking (customer sees own order) ────────────────────────────

    /**
     * GET /api/orders/{orderId}/tracking
     * Customer can only see their own order tracking.
     * Admin can see all orders (see AdminShippingController).
     */
    @GetMapping("/track/{orderId}")
    public ResponseEntity<ShippingDtos.TrackingResponse> getTracking(
            Authentication auth,
            @PathVariable Long orderId) {

        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
        if (shipment == null) {
            // No shipment created yet — return basic order info
            return ResponseEntity.ok(ShippingDtos.TrackingResponse.builder()
                .orderNumber(order.getOrderNumber())
                .shipmentStatus("Order Placed")
                .events(java.util.List.of())
                .build());
        }

        ShippingDtos.TrackingResponse tracking = shiprocketService.getTracking(shipment.getId());
        return ResponseEntity.ok(tracking);
    }

    // ─── Webhook (from Shiprocket) ───────────────────────────────────────────

    /**
     * POST /api/webhooks/shiprocket
     * Receives tracking events from Shiprocket.
     * Not authenticated via JWT — verified via webhook security token if configured.
     */
    @PostMapping("/webhooks/shiprocket")
    public ResponseEntity<Map<String, String>> shiprocketWebhook(
            @RequestHeader(value = "x-api-key", required = false) String webhookKey,
            @RequestBody Map<String, Object> payload) {

        // Basic webhook authenticity check via token if configured
        // (Token configured in Shiprocket Panel → Settings → API → Webhooks)
        String expectedKey = System.getenv("SHIPROCKET_WEBHOOK_TOKEN");
        if (expectedKey != null && !expectedKey.isBlank()) {
            if (webhookKey == null || !expectedKey.equals(webhookKey)) {
                log.warn("Shiprocket webhook received with invalid key");
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
        }

        log.info("Shiprocket webhook received");
        shiprocketService.processWebhook(payload);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    // ─── Request Records ─────────────────────────────────────────────────────

    record ServiceabilityRequest(
        @NotBlank String deliveryPincode,
        Integer weightGrams,
        Boolean cod,
        BigDecimal orderValue
    ) {}
}
