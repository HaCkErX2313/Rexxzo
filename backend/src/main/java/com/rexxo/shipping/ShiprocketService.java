package com.rexxo.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.rexxo.config.ShiprocketConfig;
import com.rexxo.entity.*;
import com.rexxo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Shiprocket implementation of LogisticsService.
 * <p>
 * This service translates between REXXO domain objects and the Shiprocket API.
 * All external HTTP calls go through ShiprocketApiClient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketService implements LogisticsService {

    private final ShiprocketApiClient apiClient;
    private final ShiprocketConfig config;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final com.rexxo.service.NotificationService notificationService;

    private static final BigDecimal DEFAULT_WEIGHT_KG = new BigDecimal("0.5");
    private static final BigDecimal DEFAULT_DIM_CM = new BigDecimal("10");

    // ─── Serviceability ──────────────────────────────────────────────────────

    @Override
    public ShippingDtos.ServiceabilityResponse checkServiceability(ShippingDtos.ServiceabilityRequest req) {
        if (!config.isConfigured()) {
            return ShippingDtos.ServiceabilityResponse.builder()
                .serviceable(false)
                .message("Shipping provider not configured")
                .couriers(List.of())
                .build();
        }

        try {
            // Weight must be in kg for Shiprocket serviceability API
            double weightKg = req.getWeightGrams() > 0
                ? req.getWeightGrams() / 1000.0
                : DEFAULT_WEIGHT_KG.doubleValue();

            Map<String, String> params = new HashMap<>();
            params.put("pickup_postcode", req.getPickupPincode());
            params.put("delivery_postcode", req.getDeliveryPincode());
            params.put("weight", String.format("%.2f", weightKg));
            params.put("cod", req.isCod() ? "1" : "0");
            if (req.getOrderValue() != null) {
                params.put("declared_value", req.getOrderValue().toPlainString());
            }
            if (req.getLengthCm() != null) params.put("length", req.getLengthCm().toPlainString());
            if (req.getWidthCm() != null) params.put("breadth", req.getWidthCm().toPlainString());
            if (req.getHeightCm() != null) params.put("height", req.getHeightCm().toPlainString());

            JsonNode response = apiClient.get("/v1/external/courier/serviceability/", params);

            return parseServiceabilityResponse(response);

        } catch (ShiprocketApiClient.ShiprocketException e) {
            log.error("Serviceability check failed: {}", e.getMessage());
            return ShippingDtos.ServiceabilityResponse.builder()
                .serviceable(false)
                .message("Unable to check serviceability: " + e.getMessage())
                .couriers(List.of())
                .build();
        }
    }

    private ShippingDtos.ServiceabilityResponse parseServiceabilityResponse(JsonNode root) {
        List<ShippingDtos.CourierOption> couriers = new ArrayList<>();

        // Shiprocket returns: { data: { available_courier_companies: [...] } }
        JsonNode data = root.path("data");
        JsonNode available = data.path("available_courier_companies");

        if (available.isArray()) {
            for (JsonNode c : available) {
                String courierId = c.path("courier_company_id").asText("");
                String courierName = c.path("courier_name").asText("Unknown");
                double rate = c.path("rate").asDouble(0);
                int etd = c.path("etd").asInt(0);
                boolean codAvail = c.path("cod").asInt(0) == 1;
                double codCharges = c.path("cod_charges").asDouble(0);

                couriers.add(ShippingDtos.CourierOption.builder()
                    .courierId(courierId)
                    .courierName(courierName)
                    .rate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP))
                    .estimatedDeliveryDays(etd)
                    .codAvailable(codAvail)
                    .codCharges(BigDecimal.valueOf(codCharges).setScale(2, RoundingMode.HALF_UP))
                    .build());
            }
        }

        boolean serviceable = !couriers.isEmpty();
        String message = serviceable
            ? couriers.size() + " courier(s) available"
            : "No couriers serviceable for this route";

        return ShippingDtos.ServiceabilityResponse.builder()
            .serviceable(serviceable)
            .message(message)
            .couriers(couriers)
            .build();
    }

    // ─── Shipment Creation ───────────────────────────────────────────────────

    @Override
    @Transactional
    public ShippingDtos.CreateShipmentResponse createShipment(ShippingDtos.CreateShipmentRequest req) {
        Order order = orderRepository.findById(req.getOrderId())
            .orElseThrow(() -> new RuntimeException("Order not found: " + req.getOrderId()));

        // Idempotency: check if shipment already exists and is not failed
        Optional<Shipment> existing = shipmentRepository.findByOrderId(req.getOrderId());
        if (existing.isPresent()) {
            Shipment s = existing.get();
            if (s.getShipmentStatus() != Shipment.ShipmentStatus.CREATION_PENDING
                && s.getShipmentStatus() != Shipment.ShipmentStatus.FAILED) {
                log.warn("Shipment already exists for order {} with status {}",
                    req.getOrderId(), s.getShipmentStatus());
                return ShippingDtos.CreateShipmentResponse.builder()
                    .success(true)
                    .providerOrderId(s.getProviderOrderId())
                    .providerShipmentId(s.getProviderShipmentId())
                    .awbNumber(s.getAwbNumber())
                    .courierName(s.getCourierName())
                    .message("Shipment already exists")
                    .build();
            }
        }

        // Save/update shipment record (status = CREATION_PENDING initially)
        Shipment shipment = existing.orElse(Shipment.builder()
            .order(order)
            .provider("SHIPROCKET")
            .pickupPincode(config.getPickupPincode())
            .deliveryPincode(order.getShippingPincode())
            .build());

        // Calculate package dimensions from order items
        PackageDimensions dims = calculatePackageDimensions(order);
        shipment.setWeightGrams(dims.weightGrams);
        shipment.setLengthCm(dims.lengthCm);
        shipment.setWidthCm(dims.widthCm);
        shipment.setHeightCm(dims.heightCm);
        shipment.setPaymentMode("COD".equals(req.getPaymentMode())
            ? Shipment.PaymentMode.COD : Shipment.PaymentMode.PREPAID);
        shipment.setCodAmount(req.getCodAmount());
        shipment.setShipmentStatus(Shipment.ShipmentStatus.CREATION_PENDING);
        shipmentRepository.save(shipment);

        try {
            // Build the Shiprocket order creation payload
            Map<String, Object> payload = buildOrderPayload(order, req, dims);

            log.info("Creating Shiprocket order for REXXZO order {}", order.getOrderNumber());
            JsonNode response = apiClient.post("/v1/external/orders/create/adhoc", payload);

            String providerOrderId = response.path("order_id").asText("");
            String providerShipmentId = response.path("shipment_id").asText("");

            if (providerOrderId.isBlank()) {
                String errorMessage = response.path("message").asText("Unknown error");
                throw new ShiprocketApiClient.ShiprocketException(
                    "Shiprocket order creation failed: " + errorMessage);
            }

            shipment.setProviderOrderId(providerOrderId);
            shipment.setProviderShipmentId(providerShipmentId);
            shipment.setShipmentStatus(Shipment.ShipmentStatus.CREATED);
            shipment.setFailureReason(null);
            shipmentRepository.save(shipment);

            log.info("Shiprocket order created: providerOrderId={}, providerShipmentId={}",
                providerOrderId, providerShipmentId);

            // Optionally assign courier if courierId is provided
            String awb = null;
            String courierName = null;
            Integer courierId = req.getCourierId();
            String trackingUrl = null;

            if (courierId != null && !providerShipmentId.isBlank()) {
                AwbResult awbResult = assignAwb(shipment, providerShipmentId, courierId);
                awb = awbResult.awb;
                courierName = awbResult.courierName;
                trackingUrl = awbResult.trackingUrl;
            }

            return ShippingDtos.CreateShipmentResponse.builder()
                .success(true)
                .providerOrderId(providerOrderId)
                .providerShipmentId(providerShipmentId)
                .awbNumber(awb)
                .courierName(courierName)
                .courierId(courierId)
                .trackingUrl(trackingUrl)
                .message("Shipment created successfully")
                .build();

        } catch (Exception e) {
            log.error("Failed to create Shiprocket shipment for order {}: {}",
                order.getOrderNumber(), e.getMessage());
            shipment.setShipmentStatus(Shipment.ShipmentStatus.FAILED);
            shipment.setFailureReason(e.getMessage());
            shipmentRepository.save(shipment);

            return ShippingDtos.CreateShipmentResponse.builder()
                .success(false)
                .message("Shipment creation failed: " + e.getMessage())
                .build();
        }
    }

    private AwbResult assignAwb(Shipment shipment, String providerShipmentId, int courierId) {
        try {
            Map<String, Object> awbPayload = Map.of(
                "shipment_id", List.of(Integer.parseInt(providerShipmentId)),
                "courier_id", courierId
            );

            log.info("Assigning AWB for shipment {}", providerShipmentId);
            JsonNode awbResponse = apiClient.post("/v1/external/courier/assign/awb", awbPayload);

            JsonNode awbNode = awbResponse.path("response").path("data");
            String awb = awbNode.path("awb_code").asText("");
            String courierName = awbNode.path("courier_name").asText("");
            String trackingUrl = awbNode.path("routing_code").asText("");

            if (!awb.isBlank()) {
                shipment.setAwbNumber(awb);
                shipment.setCourierName(courierName);
                shipment.setCourierId(courierId);
                shipment.setTrackingUrl("https://shiprocket.co/tracking/" + awb);
                shipment.setShipmentStatus(Shipment.ShipmentStatus.AWB_ASSIGNED);
                shipmentRepository.save(shipment);
                log.info("AWB assigned: {}", awb);
            }

            return new AwbResult(awb, courierName, "https://shiprocket.co/tracking/" + awb);

        } catch (Exception e) {
            log.error("AWB assignment failed for shipment {}: {}", providerShipmentId, e.getMessage());
            return new AwbResult(null, null, null);
        }
    }

    private record AwbResult(String awb, String courierName, String trackingUrl) {}

    private Map<String, Object> buildOrderPayload(Order order, ShippingDtos.CreateShipmentRequest req,
                                                   PackageDimensions dims) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("order_id", order.getOrderNumber());
        payload.put("order_date", order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        payload.put("pickup_location", "Primary");

        // Billing = Shipping for B2C
        payload.put("billing_customer_name", order.getShippingName());
        payload.put("billing_address", order.getShippingAddress());
        payload.put("billing_city", order.getShippingCity());
        payload.put("billing_pincode", order.getShippingPincode());
        payload.put("billing_state", order.getShippingState());
        payload.put("billing_country", "India");
        payload.put("billing_email", order.getUser().getEmail());
        payload.put("billing_phone", order.getShippingPhone());

        payload.put("shipping_is_billing", true);
        payload.put("shipping_customer_name", order.getShippingName());
        payload.put("shipping_address", order.getShippingAddress());
        payload.put("shipping_city", order.getShippingCity());
        payload.put("shipping_pincode", order.getShippingPincode());
        payload.put("shipping_state", order.getShippingState());
        payload.put("shipping_country", "India");
        payload.put("shipping_email", order.getUser().getEmail());
        payload.put("shipping_phone", order.getShippingPhone());

        // Payment
        payload.put("payment_method", "COD".equals(req.getPaymentMode()) ? "COD" : "Prepaid");
        if ("COD".equals(req.getPaymentMode()) && req.getCodAmount() != null) {
            payload.put("sub_total", req.getCodAmount().toPlainString());
        } else {
            payload.put("sub_total", order.getTotal().toPlainString());
        }

        // Order items
        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> oi = new LinkedHashMap<>();
            oi.put("name", item.getProductName());
            oi.put("sku", item.getProduct() != null ? item.getProduct().getSku() : item.getProductName());
            oi.put("units", item.getQuantity());
            oi.put("selling_price", item.getUnitPrice().toPlainString());
            orderItems.add(oi);
        }
        payload.put("order_items", orderItems);

        // Package dimensions (weight in kg for Shiprocket, dims in cm)
        double weightKg = dims.weightGrams / 1000.0;
        payload.put("weight", String.format("%.3f", weightKg));
        payload.put("length", dims.lengthCm.toPlainString());
        payload.put("breadth", dims.widthCm.toPlainString());
        payload.put("height", dims.heightCm.toPlainString());

        return payload;
    }

    private PackageDimensions calculatePackageDimensions(Order order) {
        int totalWeightGrams = 0;
        BigDecimal maxLength = DEFAULT_DIM_CM;
        BigDecimal maxWidth = DEFAULT_DIM_CM;
        BigDecimal maxHeight = DEFAULT_DIM_CM;

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                int itemWeight = product.getWeightGrams() != null ? product.getWeightGrams() : 500;
                totalWeightGrams += itemWeight * item.getQuantity();

                if (product.getLengthCm() != null) {
                    maxLength = maxLength.max(product.getLengthCm());
                }
                if (product.getWidthCm() != null) {
                    maxWidth = maxWidth.max(product.getWidthCm());
                }
                if (product.getHeightCm() != null) {
                    maxHeight = maxHeight.max(product.getHeightCm());
                }
            } else {
                totalWeightGrams += 500 * item.getQuantity();
            }
        }

        if (totalWeightGrams == 0) totalWeightGrams = 500;

        return new PackageDimensions(totalWeightGrams, maxLength, maxWidth, maxHeight);
    }

    private record PackageDimensions(int weightGrams, BigDecimal lengthCm,
                                      BigDecimal widthCm, BigDecimal heightCm) {}

    // ─── Tracking ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ShippingDtos.TrackingResponse getTracking(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElse(null);
        if (shipment == null) return null;

        // If AWB exists, fetch live tracking from Shiprocket
        if (shipment.getAwbNumber() != null && !shipment.getAwbNumber().isBlank()
            && !ShiprocketStatusMapper.isTerminal(shipment.getShipmentStatus())) {
            syncTrackingFromProvider(shipment);
        }

        // Build response from DB (authoritative source of truth)
        List<ShipmentTrackingEvent> events = trackingEventRepository
            .findByShipmentIdOrderByEventTimestampAsc(shipmentId);

        List<ShippingDtos.TrackingEvent> eventDtos = events.stream()
            .map(e -> ShippingDtos.TrackingEvent.builder()
                .status(e.getStatus())
                .rawStatus(e.getRawStatus())
                .description(e.getDescription())
                .location(e.getLocation())
                .eventTimestamp(e.getEventTimestamp() != null
                    ? e.getEventTimestamp().toString() : null)
                .build())
            .toList();

        ShippingDtos.TrackingEvent latestEvent = eventDtos.isEmpty()
            ? null : eventDtos.get(eventDtos.size() - 1);

        return ShippingDtos.TrackingResponse.builder()
            .orderNumber(shipment.getOrder().getOrderNumber())
            .shipmentStatus(ShiprocketStatusMapper.toDisplayStatus(shipment.getShipmentStatus()))
            .awbNumber(shipment.getAwbNumber())
            .courierName(shipment.getCourierName())
            .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate() != null
                ? shipment.getEstimatedDeliveryDate().toString() : null)
            .trackingUrl(shipment.getTrackingUrl())
            .latestEvent(latestEvent)
            .events(eventDtos)
            .build();
    }

    @Transactional
    public void syncTrackingFromProvider(Shipment shipment) {
        if (shipment.getAwbNumber() == null || shipment.getAwbNumber().isBlank()) return;

        try {
            Map<String, String> params = Map.of("awb", shipment.getAwbNumber());
            JsonNode response = apiClient.get("/v1/external/courier/track/awb/" + shipment.getAwbNumber(), null);

            JsonNode trackingData = response.path("tracking_data");
            if (trackingData.isMissingNode()) {
                log.debug("No tracking data yet for AWB {}", shipment.getAwbNumber());
                return;
            }

            // Update shipment status
            String currentStatus = trackingData.path("shipment_status").asText("");
            if (!currentStatus.isBlank()) {
                Shipment.ShipmentStatus newStatus = ShiprocketStatusMapper.toShipmentStatus(currentStatus);
                if (newStatus != shipment.getShipmentStatus()) {
                    log.info("Shipment {} status: {} -> {}", shipment.getId(),
                        shipment.getShipmentStatus(), newStatus);
                    shipment.setShipmentStatus(newStatus);
                    shipment.setRawProviderStatus(currentStatus);

                    // Update terminal timestamps
                    if (newStatus == Shipment.ShipmentStatus.DELIVERED) {
                        if (shipment.getDeliveredAt() == null) shipment.setDeliveredAt(LocalDateTime.now());
                        if (shipment.getOrder() != null) {
                            shipment.getOrder().setStatus(Order.OrderStatus.DELIVERED);
                            orderRepository.save(shipment.getOrder());
                        }
                        notificationService.notifyDelivered(shipment);
                    } else if (newStatus == Shipment.ShipmentStatus.OUT_FOR_DELIVERY) {
                        if (shipment.getOrder() != null) {
                            shipment.getOrder().setStatus(Order.OrderStatus.OUT_FOR_DELIVERY);
                            orderRepository.save(shipment.getOrder());
                        }
                        notificationService.notifyOutForDelivery(shipment);
                    } else if (newStatus == Shipment.ShipmentStatus.PICKED_UP || newStatus == Shipment.ShipmentStatus.IN_TRANSIT) {
                        if (shipment.getShippedAt() == null) shipment.setShippedAt(LocalDateTime.now());
                        if (shipment.getOrder() != null) {
                            shipment.getOrder().setStatus(Order.OrderStatus.SHIPPED);
                            orderRepository.save(shipment.getOrder());
                        }
                        notificationService.notifyShipmentDispatched(shipment);
                    }
                }
            }

            // Parse and store tracking events
            JsonNode activities = trackingData.path("shipment_track_activities");
            if (activities.isArray()) {
                for (JsonNode activity : activities) {
                    processTrackingActivity(shipment, activity);
                }
            }

            shipmentRepository.save(shipment);

        } catch (Exception e) {
            log.warn("Failed to sync tracking for AWB {}: {}", shipment.getAwbNumber(), e.getMessage());
            // Don't throw — tracking sync failure should not break the request
        }
    }

    private void processTrackingActivity(Shipment shipment, JsonNode activity) {
        String rawStatus = activity.path("activity").asText("");
        String location = activity.path("location").asText("");
        String dateStr = activity.path("date").asText("");

        if (rawStatus.isBlank()) return;

        // Build a deduplication key from the composite of status + location + date
        String eventId = (rawStatus + "|" + location + "|" + dateStr).toLowerCase().replaceAll("\\s+", " ");

        if (trackingEventRepository.existsByShipmentIdAndProviderEventId(shipment.getId(), eventId)) {
            return; // Already stored
        }

        LocalDateTime eventTime = parseDateTime(dateStr);

        ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
            .shipment(shipment)
            .providerEventId(eventId)
            .rawStatus(rawStatus)
            .status(ShiprocketStatusMapper.toShipmentStatus(rawStatus).name())
            .description(rawStatus)
            .location(location)
            .eventTimestamp(eventTime)
            .build();

        trackingEventRepository.save(event);
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDateTime.now();
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter fmt : formatters) {
            try { return LocalDateTime.parse(dateStr, fmt); } catch (DateTimeParseException ignored) {}
        }
        return LocalDateTime.now();
    }

    // ─── Cancel / Label ──────────────────────────────────────────────────────

    @Override
    public boolean cancelShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null) return false;

        if (shipment.getProviderOrderId() == null) {
            // Not yet created in Shiprocket — just mark cancelled locally
            shipment.setShipmentStatus(Shipment.ShipmentStatus.CANCELLED);
            shipmentRepository.save(shipment);
            return true;
        }

        try {
            Map<String, Object> payload = Map.of(
                "ids", List.of(Integer.parseInt(shipment.getProviderOrderId()))
            );
            apiClient.post("/v1/external/orders/cancel", payload);
            shipment.setShipmentStatus(Shipment.ShipmentStatus.CANCELLED);
            shipmentRepository.save(shipment);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel shipment {}: {}", shipmentId, e.getMessage());
            return false;
        }
    }

    @Override
    public String generateLabel(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || shipment.getProviderShipmentId() == null) return null;

        try {
            Map<String, Object> payload = Map.of(
                "shipment_id", List.of(Integer.parseInt(shipment.getProviderShipmentId()))
            );
            JsonNode response = apiClient.post("/v1/external/courier/generate/label", payload);
            return response.path("label_url").asText(null);
        } catch (Exception e) {
            log.error("Label generation failed for shipment {}: {}", shipmentId, e.getMessage());
            return null;
        }
    }

    // ─── Health ──────────────────────────────────────────────────────────────

    @Override
    public boolean isHealthy() {
        if (!config.isConfigured()) return false;
        try {
            // Attempt to get/validate a token — authenticate() logs SUCCESS or FAILED
            apiClient.getBearerToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Processes a Shiprocket webhook payload.
     * Called by ShippingController — transaction managed here.
     */
    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        // Shiprocket sends awb and current_status in webhook body
        Object awbObj = payload.get("awb");
        Object statusObj = payload.get("current_status");

        if (awbObj == null || statusObj == null) {
            log.warn("Webhook missing awb or current_status");
            return;
        }

        String awb = awbObj.toString();
        String rawStatus = statusObj.toString();

        Shipment shipment = shipmentRepository.findByAwbNumber(awb).orElse(null);
        if (shipment == null) {
            log.warn("Webhook received for unknown AWB: {}", awb);
            return;
        }

        Shipment.ShipmentStatus newStatus = ShiprocketStatusMapper.toShipmentStatus(rawStatus);
        if (newStatus != shipment.getShipmentStatus()) {
            log.info("Webhook: shipment {} status {} -> {}", awb, shipment.getShipmentStatus(), newStatus);
            shipment.setShipmentStatus(newStatus);
            shipment.setRawProviderStatus(rawStatus);

            // Update associated order status
            Order order = shipment.getOrder();
            Order.OrderStatus newOrderStatus = ShiprocketStatusMapper.toOrderStatus(newStatus);
            order.setStatus(newOrderStatus);

            if (newStatus == Shipment.ShipmentStatus.DELIVERED) {
                shipment.setDeliveredAt(LocalDateTime.now());
            }
        }

        // Store tracking event from webhook
        String location = payload.containsKey("current_status_detail") ?
            payload.get("current_status_detail").toString() : "";
        String dateStr = payload.containsKey("updated_at") ?
            payload.get("updated_at").toString() : "";

        String eventId = (rawStatus + "|" + location + "|" + dateStr).toLowerCase();
        if (!trackingEventRepository.existsByShipmentIdAndProviderEventId(shipment.getId(), eventId)) {
            ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
                .shipment(shipment)
                .providerEventId(eventId)
                .rawStatus(rawStatus)
                .status(newStatus.name())
                .description(rawStatus)
                .location(location)
                .eventTimestamp(parseDateTime(dateStr))
                .build();
            trackingEventRepository.save(event);
        }

        shipmentRepository.save(shipment);
    }
}
