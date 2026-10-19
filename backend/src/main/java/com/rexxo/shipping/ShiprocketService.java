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

    private final ShiprocketClient apiClient;
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

        // Resolve payment mode and COD amount from request or persisted order
        String paymentMode = req.getPaymentMode();
        if (paymentMode == null || paymentMode.isBlank()) {
            paymentMode = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? "COD" : "PREPAID";
        }
        boolean isCod = "COD".equalsIgnoreCase(paymentMode);
        BigDecimal codAmount = isCod
            ? (req.getCodAmount() != null ? req.getCodAmount() : order.getTotal())
            : BigDecimal.ZERO;

        // Idempotency: check if shipment already exists with a valid providerOrderId
        Optional<Shipment> existing = shipmentRepository.findByOrderId(req.getOrderId());
        if (existing.isPresent()) {
            Shipment s = existing.get();
            if (s.getProviderOrderId() != null && !s.getProviderOrderId().isBlank()) {
                log.info("Shipment already exists for order {} with providerOrderId {}",
                    order.getOrderNumber(), s.getProviderOrderId());
                return ShippingDtos.CreateShipmentResponse.builder()
                    .success(true)
                    .providerOrderId(s.getProviderOrderId())
                    .providerShipmentId(s.getProviderShipmentId())
                    .awbNumber(s.getAwbNumber())
                    .courierName(s.getCourierName())
                    .courierId(s.getCourierId())
                    .trackingUrl(s.getTrackingUrl())
                    .message("Shipment already exists")
                    .build();
            }
        }

        // Initialize / update shipment record
        Shipment shipment = existing.orElseGet(() -> Shipment.builder()
            .order(order)
            .provider("SHIPROCKET")
            .pickupPincode(config.getPickupPincode())
            .deliveryPincode(order.getShippingPincode())
            .build());

        shipment.setAttemptCount((shipment.getAttemptCount() != null ? shipment.getAttemptCount() : 0) + 1);
        shipment.setLastAttemptAt(LocalDateTime.now());
        shipment.setPaymentMode(isCod ? Shipment.PaymentMode.COD : Shipment.PaymentMode.PREPAID);
        shipment.setCodAmount(codAmount);
        shipment.setShipmentStatus(Shipment.ShipmentStatus.CREATION_PENDING);
        shipmentRepository.save(shipment);

        // Fail-safe check for credentials
        if (!config.isConfigured()) {
            String errorMsg = "Shiprocket credentials are not configured. Configure SHIPROCKET_EMAIL and SHIPROCKET_PASSWORD in Render backend environment variables.";
            log.warn("Cannot create Shiprocket order for {}: {}", order.getOrderNumber(), errorMsg);
            shipment.setShipmentStatus(Shipment.ShipmentStatus.FAILED);
            shipment.setFailureReason(errorMsg);
            shipmentRepository.save(shipment);
            return ShippingDtos.CreateShipmentResponse.builder()
                .success(false)
                .message(errorMsg)
                .build();
        }

        // Calculate package dimensions
        PackageDimensions dims = calculatePackageDimensions(order);
        shipment.setWeightGrams(dims.weightGrams);
        shipment.setLengthCm(dims.lengthCm);
        shipment.setWidthCm(dims.widthCm);
        shipment.setHeightCm(dims.heightCm);

        // Validate payload data before hitting Shiprocket
        String validationError = validateOrderForShipping(order);
        if (validationError != null) {
            log.error("Shipping validation failed for order {}: {}", order.getOrderNumber(), validationError);
            shipment.setShipmentStatus(Shipment.ShipmentStatus.FAILED);
            shipment.setFailureReason("Validation error: " + validationError);
            shipmentRepository.save(shipment);
            return ShippingDtos.CreateShipmentResponse.builder()
                .success(false)
                .message("Shipping validation failed: " + validationError)
                .build();
        }

        // Ambiguous retry protection: Check if order already exists in Shiprocket before creating
        if (shipment.getAttemptCount() > 1) {
            ReconciliationResult reconciled = reconcileExistingOrderInShiprocket(order.getOrderNumber());
            if (reconciled != null) {
                log.info("Shiprocket order previously created for {}: providerOrderId={}, providerShipmentId={}",
                    order.getOrderNumber(), reconciled.providerOrderId(), reconciled.providerShipmentId());
                shipment.setProviderOrderId(reconciled.providerOrderId());
                shipment.setProviderShipmentId(reconciled.providerShipmentId());
                shipment.setAwbNumber(reconciled.awbNumber());
                shipment.setCourierName(reconciled.courierName());
                shipment.setShipmentStatus(Shipment.ShipmentStatus.CREATED);
                shipment.setFailureReason(null);
                shipmentRepository.save(shipment);

                order.setShipment(shipment);
                orderRepository.save(order);

                return ShippingDtos.CreateShipmentResponse.builder()
                    .success(true)
                    .providerOrderId(reconciled.providerOrderId())
                    .providerShipmentId(reconciled.providerShipmentId())
                    .awbNumber(reconciled.awbNumber())
                    .courierName(reconciled.courierName())
                    .message("Shipment reconciled with existing Shiprocket order")
                    .build();
            }
        }

        try {
            // Build the compliant Shiprocket order creation payload
            Map<String, Object> payload = buildOrderPayload(order, isCod, codAmount, dims);

            log.info("Creating Shiprocket order for REXXZO order {} (Payment: {}, Pickup: {})",
                order.getOrderNumber(), isCod ? "COD" : "Prepaid", config.getPickupLocation());
            JsonNode response = apiClient.post("/v1/external/orders/create/adhoc", payload);

            String providerOrderId = response.path("order_id").asText("");
            String providerShipmentId = response.path("shipment_id").asText("");

            if (providerOrderId.isBlank()) {
                String errorMessage = response.path("message").asText("Unknown response from Shiprocket");
                throw new ShiprocketApiClient.ShiprocketException(
                    "Shiprocket order creation failed: " + errorMessage);
            }

            shipment.setProviderOrderId(providerOrderId);
            shipment.setProviderShipmentId(providerShipmentId);
            shipment.setShipmentStatus(Shipment.ShipmentStatus.CREATED);
            shipment.setFailureReason(null);
            shipmentRepository.save(shipment);

            order.setShipment(shipment);
            orderRepository.save(order);

            log.info("Shiprocket order created successfully: providerOrderId={}, providerShipmentId={}",
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

            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";

            // If error indicates the order already exists in Shiprocket, reconcile rather than leaving failed
            if (errorMsg.toLowerCase().contains("already exist") || errorMsg.toLowerCase().contains("duplicate")) {
                ReconciliationResult reconciled = reconcileExistingOrderInShiprocket(order.getOrderNumber());
                if (reconciled != null) {
                    log.info("Successfully reconciled duplicate order {} with Shiprocket", order.getOrderNumber());
                    shipment.setProviderOrderId(reconciled.providerOrderId());
                    shipment.setProviderShipmentId(reconciled.providerShipmentId());
                    shipment.setAwbNumber(reconciled.awbNumber());
                    shipment.setCourierName(reconciled.courierName());
                    shipment.setShipmentStatus(Shipment.ShipmentStatus.CREATED);
                    shipment.setFailureReason(null);
                    shipmentRepository.save(shipment);

                    order.setShipment(shipment);
                    orderRepository.save(order);

                    return ShippingDtos.CreateShipmentResponse.builder()
                        .success(true)
                        .providerOrderId(reconciled.providerOrderId())
                        .providerShipmentId(reconciled.providerShipmentId())
                        .awbNumber(reconciled.awbNumber())
                        .courierName(reconciled.courierName())
                        .message("Shipment reconciled with existing Shiprocket order")
                        .build();
                }
            }

            shipment.setShipmentStatus(Shipment.ShipmentStatus.FAILED);
            shipment.setFailureReason(errorMsg);
            shipmentRepository.save(shipment);

            return ShippingDtos.CreateShipmentResponse.builder()
                .success(false)
                .message("Shipment creation failed: " + errorMsg)
                .build();
        }
    }

    private record ReconciliationResult(String providerOrderId, String providerShipmentId, String awbNumber, String courierName) {}

    private ReconciliationResult reconcileExistingOrderInShiprocket(String orderNumber) {
        if (!config.isConfigured() || orderNumber == null || orderNumber.isBlank()) return null;
        try {
            Map<String, String> query = Map.of("search", orderNumber);
            JsonNode response = apiClient.get("/v1/external/orders", query);
            JsonNode data = response.path("data");
            if (data.isArray()) {
                for (JsonNode ordNode : data) {
                    String channelOrderId = ordNode.path("channel_order_id").asText("");
                    if (orderNumber.equalsIgnoreCase(channelOrderId)) {
                        String provOrdId = ordNode.path("id").asText("");
                        String provShipId = "";
                        String awb = "";
                        String courier = "";
                        JsonNode shipmentsNode = ordNode.path("shipments");
                        if (shipmentsNode.isArray() && shipmentsNode.size() > 0) {
                            JsonNode firstShip = shipmentsNode.get(0);
                            provShipId = firstShip.path("id").asText("");
                            awb = firstShip.path("awb").asText("");
                            courier = firstShip.path("courier_name").asText("");
                        }
                        if (!provOrdId.isBlank()) {
                            return new ReconciliationResult(provOrdId, provShipId, awb, courier);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Reconciliation query failed for order {}: {}", orderNumber, e.getMessage());
        }
        return null;
    }

    private String validateOrderForShipping(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "Order has no items to ship";
        }
        String pincode = sanitizePincode(order.getShippingPincode());
        if (pincode == null || pincode.length() != 6) {
            return "Invalid shipping pincode (must be a valid 6-digit Indian pincode): " + order.getShippingPincode();
        }
        if (order.getShippingAddress() == null || order.getShippingAddress().trim().isBlank()) {
            return "Missing delivery street address";
        }
        if (order.getShippingCity() == null || order.getShippingCity().trim().isBlank()) {
            return "Missing delivery city";
        }
        if (order.getShippingState() == null || order.getShippingState().trim().isBlank()) {
            return "Missing delivery state";
        }
        String phone = sanitizePhone(order.getShippingPhone());
        if (phone == null || phone.length() != 10) {
            return "Invalid customer phone number (must be a valid 10-digit number): " + order.getShippingPhone();
        }
        if (config.getPickupLocation() == null || config.getPickupLocation().trim().isBlank()) {
            return "Shiprocket pickup location nickname is not configured";
        }
        return null;
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

    private String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits.length() == 10 ? digits : null;
    }

    private String sanitizePincode(String pincode) {
        if (pincode == null || pincode.isBlank()) return null;
        String digits = pincode.replaceAll("[^0-9]", "");
        return digits.length() == 6 ? digits : null;
    }

    private Map<String, Object> buildOrderPayload(Order order, boolean isCod, BigDecimal codAmount,
                                                   PackageDimensions dims) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("order_id", order.getOrderNumber());
        LocalDateTime createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        payload.put("order_date", createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        payload.put("pickup_location", config.getPickupLocation());

        String rawName = (order.getShippingName() != null && !order.getShippingName().trim().isBlank())
            ? order.getShippingName().trim() : "Valued Customer";

        String firstName = rawName;
        String lastName = ".";
        if (rawName.contains(" ")) {
            int idx = rawName.lastIndexOf(" ");
            firstName = rawName.substring(0, idx).trim();
            lastName = rawName.substring(idx + 1).trim();
            if (lastName.isBlank()) lastName = ".";
        }

        String customerPhone = sanitizePhone(order.getShippingPhone());
        String customerEmail = (order.getUser() != null && order.getUser().getEmail() != null)
            ? order.getUser().getEmail() : "orders@rexxzo.com";
        String shippingAddress = order.getShippingAddress() != null ? order.getShippingAddress().trim() : "";
        String city = order.getShippingCity() != null ? order.getShippingCity().trim() : "";
        String state = order.getShippingState() != null ? order.getShippingState().trim() : "";
        String pincode = sanitizePincode(order.getShippingPincode());

        // Billing = Shipping for B2C
        payload.put("billing_customer_name", firstName);
        payload.put("billing_last_name", lastName);
        payload.put("billing_address", shippingAddress);
        payload.put("billing_city", city);
        payload.put("billing_pincode", pincode);
        payload.put("billing_state", state);
        payload.put("billing_country", "India");
        payload.put("billing_email", customerEmail);
        payload.put("billing_phone", customerPhone);

        payload.put("shipping_is_billing", true);
        payload.put("shipping_customer_name", firstName);
        payload.put("shipping_last_name", lastName);
        payload.put("shipping_address", shippingAddress);
        payload.put("shipping_city", city);
        payload.put("shipping_pincode", pincode);
        payload.put("shipping_state", state);
        payload.put("shipping_country", "India");
        payload.put("shipping_email", customerEmail);
        payload.put("shipping_phone", customerPhone);

        // Payment Mode: "COD" or "Prepaid"
        payload.put("payment_method", isCod ? "COD" : "Prepaid");

        // Subtotal, shipping charge, discount
        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;

        payload.put("sub_total", subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
        payload.put("shipping_charges", shippingFee.setScale(2, RoundingMode.HALF_UP).toPlainString());
        payload.put("total_discount", discount.setScale(2, RoundingMode.HALF_UP).toPlainString());

        // Order items
        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> oi = new LinkedHashMap<>();
            String itemName = item.getProductName() != null && !item.getProductName().isBlank()
                ? item.getProductName() : "REXXZO Item";
            String sku = (item.getProduct() != null && item.getProduct().getSku() != null && !item.getProduct().getSku().isBlank())
                ? item.getProduct().getSku() : ("SKU-" + (item.getProduct() != null ? item.getProduct().getId() : item.getId()));
            oi.put("name", itemName);
            oi.put("sku", sku);
            oi.put("units", item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1);
            oi.put("selling_price", item.getUnitPrice() != null
                ? item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00");
            oi.put("discount", "0.00");
            oi.put("tax", "0.00");
            orderItems.add(oi);
        }
        payload.put("order_items", orderItems);

        // Package dimensions (weight in kg with dot decimal format, dims in cm)
        double weightKg = Math.max(dims.weightGrams / 1000.0, 0.1);
        BigDecimal length = dims.lengthCm.max(new BigDecimal("5"));
        BigDecimal width = dims.widthCm.max(new BigDecimal("5"));
        BigDecimal height = dims.heightCm.max(new BigDecimal("5"));

        payload.put("weight", String.format(Locale.ROOT, "%.3f", weightKg));
        payload.put("length", length.toPlainString());
        payload.put("breadth", width.toPlainString());
        payload.put("height", height.toPlainString());

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
            if (response == null) {
                log.debug("No response from provider for AWB {}", shipment.getAwbNumber());
                return;
            }

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

    // ─── Health & Diagnostics ───────────────────────────────────────────────

    @Override
    public boolean isHealthy() {
        if (!config.isConfigured()) return false;
        try {
            apiClient.getBearerToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detailed authentication diagnostic test without exposing any credentials.
     */
    public Map<String, Object> testAuthentication() {
        if (!config.isConfigured()) {
            return Map.of(
                "configured", false,
                "authentication", "FAILED",
                "message", "Shiprocket credentials are not configured in backend environment variables.",
                "baseUrl", config.getBaseUrl()
            );
        }
        try {
            apiClient.getBearerToken();
            return Map.of(
                "configured", true,
                "authentication", "OK",
                "message", "Shiprocket authentication verified successfully. API token active.",
                "baseUrl", config.getBaseUrl(),
                "pickupPincode", config.getPickupPincode(),
                "pickupLocation", config.getPickupLocation()
            );
        } catch (Exception e) {
            String err = apiClient.getLastAuthError();
            if (err == null || err.isBlank()) err = e.getMessage();
            return Map.of(
                "configured", true,
                "authentication", "FAILED",
                "message", err != null ? err : "Authentication failed",
                "baseUrl", config.getBaseUrl(),
                "pickupPincode", config.getPickupPincode(),
                "pickupLocation", config.getPickupLocation()
            );
        }
    }

    /**
     * Queries Shiprocket for configured pickup locations.
     */
    public JsonNode getPickupLocations() {
        if (!config.isConfigured()) return null;
        try {
            return apiClient.get("/v1/external/settings/company/pickup", null);
        } catch (Exception e) {
            log.error("Failed to fetch pickup locations from Shiprocket: {}", e.getMessage());
            return null;
        }
    }

    /**
     * On-demand courier and AWB assignment for an existing shipment.
     */
    @Transactional
    public ShippingDtos.CreateShipmentResponse assignCourier(Long shipmentId, Integer courierId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new RuntimeException("Shipment not found: " + shipmentId));
        if (shipment.getProviderShipmentId() == null || shipment.getProviderShipmentId().isBlank()) {
            throw new RuntimeException("Cannot assign courier: Order has not been created in Shiprocket yet.");
        }
        AwbResult result = assignAwb(shipment, shipment.getProviderShipmentId(), courierId);
        return ShippingDtos.CreateShipmentResponse.builder()
            .success(result.awb != null)
            .providerOrderId(shipment.getProviderOrderId())
            .providerShipmentId(shipment.getProviderShipmentId())
            .awbNumber(result.awb)
            .courierName(result.courierName)
            .courierId(courierId)
            .trackingUrl(result.trackingUrl)
            .message(result.awb != null ? "AWB assigned successfully" : "Failed to assign courier/AWB")
            .build();
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
