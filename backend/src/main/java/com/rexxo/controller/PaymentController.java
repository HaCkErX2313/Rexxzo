package com.rexxo.controller;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import com.rexxo.service.AnalyticsService;
import com.rexxo.service.NotificationService;
import com.rexxo.shipping.ShippingDtos;
import com.rexxo.shipping.ShiprocketService;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Handles Razorpay payment lifecycle:
 * 1. Create Razorpay order (returns order_id and key_id to frontend)
 * 2. Cryptographic signature verification (server-side HMAC-SHA256)
 * 3. Idempotent webhook event processing (payment.captured, order.paid, payment.failed)
 * 4. Confirmation of Cash on Delivery (COD) orders
 *
 * CRITICAL: Payment is only marked successful after cryptographic
 * signature verification on the backend or verified webhook.
 * Frontend payment success reports are NEVER trusted directly.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook.secret:}")
    private String razorpayWebhookSecret;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShiprocketService shiprocketService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    // ─── Create Razorpay Order ────────────────────────────────────────────────

    /**
     * POST /api/payments/create-order
     * Creates a Razorpay order for the given REXXO order.
     * Frontend uses the returned razorpayOrderId and keyId to open the Razorpay checkout modal.
     */
    @PostMapping("/create-order")
    @Transactional
    public ResponseEntity<CreateOrderResponse> createRazorpayOrder(
            Authentication auth,
            @Valid @RequestBody CreateOrderRequest request) {

        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(request.orderId(), user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found with ID: " + request.orderId()));

        if (order.getStatus() != Order.OrderStatus.PENDING
            && order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw RexxoException.badRequest("Order is not in a payable state: " + order.getStatus());
        }

        long amountPaisa = order.getTotal()
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();

        // Check if Razorpay order was already created for this order (Idempotency)
        Payment existingPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getRazorpayOrderId() != null) {
            return ResponseEntity.ok(new CreateOrderResponse(
                existingPayment.getRazorpayOrderId(),
                amountPaisa,
                "INR",
                razorpayKeyId
            ));
        }

        String razorpayOrderId;
        if (isRazorpayConfigured()) {
            try {
                RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amountPaisa);
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", order.getOrderNumber());

                com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
                razorpayOrderId = razorpayOrder.get("id");
            } catch (RazorpayException e) {
                log.error("Failed to create Razorpay order for {}: {}", order.getOrderNumber(), e.getMessage());
                throw RexxoException.badRequest("Payment gateway initialization failed: " + e.getMessage());
            }
        } else {
            // Development / Test mode fallback
            log.warn("Razorpay credentials are placeholder/unset. Generating mock order ID for development/testing.");
            razorpayOrderId = "order_mock_" + order.getId() + "_" + System.currentTimeMillis();
        }

        // Persist Payment record
        Payment payment = existingPayment != null ? existingPayment :
            Payment.builder().order(order).build();
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmount(order.getTotal());
        payment.setCurrency("INR");
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return ResponseEntity.ok(new CreateOrderResponse(
            razorpayOrderId,
            amountPaisa,
            "INR",
            razorpayKeyId
        ));
    }

    // ─── Verify Payment ───────────────────────────────────────────────────────

    /**
     * POST /api/payments/verify
     * Cryptographically verifies Razorpay payment signature via HMAC-SHA256.
     * This is the authoritative path where a REXXO order gets marked PAID.
     */
    @PostMapping("/verify")
    @Transactional(noRollbackFor = RexxoException.class)
    public ResponseEntity<VerifyResponse> verifyPayment(
            Authentication auth,
            @Valid @RequestBody VerifyRequest request) {

        User user = getUser(auth);

        // 1. Load the payment and verify user ownership
        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
            .orElseThrow(() -> RexxoException.notFound("Payment record not found for Razorpay Order: " + request.razorpayOrderId()));

        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("Order does not belong to the authenticated user");
        }

        // Idempotency: If already verified and marked PAID, return success immediately
        if (payment.getStatus() == Payment.PaymentStatus.PAID && order.getStatus() == Order.OrderStatus.PAID) {
            return ResponseEntity.ok(new VerifyResponse(
                true,
                order.getOrderNumber(),
                "Payment already verified and order confirmed."
            ));
        }

        // 2. Cryptographic signature verification
        boolean signatureValid = verifyRazorpaySignature(
            request.razorpayOrderId(),
            request.razorpayPaymentId(),
            request.razorpaySignature()
        );

        if (!signatureValid) {
            log.warn("Razorpay signature verification FAILED for order {}", order.getOrderNumber());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw RexxoException.badRequest("Payment verification failed. Invalid signature.");
        }

        // 3. Mark payment and order as PAID
        log.info("Payment verified successfully for order {}", order.getOrderNumber());
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setStatus(Payment.PaymentStatus.PAID);
        paymentRepository.save(payment);

        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentMethod("PREPAID");
        orderRepository.save(order);

        // 4. Clear customer cart upon confirmed payment
        cartRepository.findByUserId(user.getId()).ifPresent(c -> {
            cartItemRepository.deleteByCartId(c.getId());
        });

        // 5. Customer Notification & Analytics
        notificationService.notifyOrderConfirmed(order);
        analyticsService.trackEvent(AnalyticsEvent.EventType.ORDER_CREATED, user.getId(), null,
            "Order", order.getId(), "paid=" + order.getTotal());

        // 6. Trigger Shiprocket shipment creation (best effort)
        try {
            ShippingDtos.CreateShipmentRequest shipReq = ShippingDtos.CreateShipmentRequest.builder()
                .orderId(order.getId())
                .paymentMode("PREPAID")
                .build();
            shiprocketService.createShipment(shipReq);
        } catch (Exception e) {
            log.error("Shiprocket shipment creation failed after payment for order {}: {}",
                order.getOrderNumber(), e.getMessage());
        }

        return ResponseEntity.ok(new VerifyResponse(
            true,
            order.getOrderNumber(),
            "Payment successful. Your order is confirmed."
        ));
    }

    // ─── Razorpay Webhook ─────────────────────────────────────────────────────

    /**
     * POST /api/payments/webhook
     * Handles asynchronous server-to-server webhook notifications from Razorpay.
     * Verifies X-Razorpay-Signature using RAZORPAY_WEBHOOK_SECRET.
     */
    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        log.info("Received Razorpay webhook notification");

        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook rejected: missing X-Razorpay-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", "Missing X-Razorpay-Signature"));
        }

        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            log.warn("Razorpay webhook received but RAZORPAY_WEBHOOK_SECRET is not set in environment");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", "Webhook secret not configured on server"));
        }

        // Cryptographic HMAC-SHA256 verification of the raw request payload
        boolean isValid = verifyWebhookSignature(rawBody, signature, razorpayWebhookSecret);
        if (!isValid) {
            log.warn("Invalid Razorpay webhook signature detected");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", "Invalid webhook signature"));
        }

        try {
            JSONObject eventObj = new JSONObject(rawBody);
            String event = eventObj.optString("event");
            log.info("Processing verified Razorpay webhook event: {}", event);

            JSONObject payload = eventObj.optJSONObject("payload");
            if (payload == null) {
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "no payload"));
            }

            JSONObject paymentEntity = null;
            if (payload.has("payment")) {
                paymentEntity = payload.getJSONObject("payment").optJSONObject("entity");
            }

            JSONObject orderEntity = null;
            if (payload.has("order")) {
                orderEntity = payload.getJSONObject("order").optJSONObject("entity");
            }

            String rzpOrderId = null;
            if (paymentEntity != null && paymentEntity.has("order_id")) {
                rzpOrderId = paymentEntity.optString("order_id");
            } else if (orderEntity != null && orderEntity.has("id")) {
                rzpOrderId = orderEntity.optString("id");
            }

            String rzpPaymentId = paymentEntity != null ? paymentEntity.optString("id") : null;

            if (rzpOrderId == null || rzpOrderId.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "missing order_id"));
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrderId).orElse(null);
            if (payment == null) {
                log.warn("Webhook received for unrecognized Razorpay order: {}", rzpOrderId);
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "unknown order"));
            }

            Order order = payment.getOrder();

            switch (event) {
                case "payment.captured":
                case "order.paid":
                    // Idempotent: Only mark PAID if not already marked PAID
                    if (payment.getStatus() != Payment.PaymentStatus.PAID) {
                        if (rzpPaymentId != null && !rzpPaymentId.isBlank()) {
                            payment.setRazorpayPaymentId(rzpPaymentId);
                        }
                        payment.setStatus(Payment.PaymentStatus.PAID);
                        paymentRepository.save(payment);

                        order.setStatus(Order.OrderStatus.PAID);
                        order.setPaymentMethod("PREPAID");
                        orderRepository.save(order);

                        // Clear customer's cart
                        if (order.getUser() != null) {
                            cartRepository.findByUserId(order.getUser().getId()).ifPresent(c -> {
                                cartItemRepository.deleteByCartId(c.getId());
                            });
                        }

                        notificationService.notifyOrderConfirmed(order);

                        // Safe trigger of Shiprocket shipment creation (best effort & idempotent)
                        try {
                            ShippingDtos.CreateShipmentRequest shipReq = ShippingDtos.CreateShipmentRequest.builder()
                                .orderId(order.getId())
                                .paymentMode("PREPAID")
                                .build();
                            shiprocketService.createShipment(shipReq);
                        } catch (Exception e) {
                            log.error("Webhook: Failed to create Shiprocket shipment for order {}: {}",
                                order.getOrderNumber(), e.getMessage());
                        }

                        log.info("Webhook: Order #{} successfully marked PAID via event {}", order.getOrderNumber(), event);
                    } else {
                        log.info("Webhook: Order #{} was already PAID (idempotent skip)", order.getOrderNumber());
                    }
                    break;

                case "payment.failed":
                    // Protect against late failures: Never overwrite confirmed PAID orders
                    if (payment.getStatus() != Payment.PaymentStatus.PAID) {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        String reason = paymentEntity != null
                            ? paymentEntity.optString("error_description", "Payment failed")
                            : "Payment failed";
                        payment.setFailureReason(reason);
                        paymentRepository.save(payment);
                        log.info("Webhook: Payment failed for order #{}: {}", order.getOrderNumber(), reason);
                    } else {
                        log.info("Webhook: Ignoring late payment.failed for already PAID order #{}", order.getOrderNumber());
                    }
                    break;

                default:
                    log.debug("Unhandled webhook event: {}", event);
                    break;
            }

            return ResponseEntity.ok(Map.of("status", "processed", "event", event));

        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ─── COD Order ────────────────────────────────────────────────────────────

    /**
     * POST /api/payments/confirm-cod
     * Confirms a COD order. Courier collects payment on parcel delivery.
     */
    @PostMapping("/confirm-cod")
    @Transactional
    public ResponseEntity<VerifyResponse> confirmCodOrder(
            Authentication auth,
            @Valid @RequestBody CodConfirmRequest request) {

        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(request.orderId(), user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found with ID: " + request.orderId()));

        if (order.getStatus() != Order.OrderStatus.PENDING
            && order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw RexxoException.badRequest("Order is not in a confirmable state: " + order.getStatus());
        }

        // Create COD payment record
        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .currency("INR")
            .method(Payment.PaymentMethod.COD)
            .status(Payment.PaymentStatus.PENDING)
            .build();
        paymentRepository.save(payment);

        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setPaymentMethod("COD");
        orderRepository.save(order);

        // Clear cart
        cartRepository.findByUserId(user.getId()).ifPresent(c -> {
            cartItemRepository.deleteByCartId(c.getId());
        });

        // Create Shiprocket COD shipment
        try {
            ShippingDtos.CreateShipmentRequest shipReq = ShippingDtos.CreateShipmentRequest.builder()
                .orderId(order.getId())
                .paymentMode("COD")
                .codAmount(order.getTotal())
                .build();
            shiprocketService.createShipment(shipReq);
        } catch (Exception e) {
            log.error("Shiprocket COD shipment creation failed for order {}: {}",
                order.getOrderNumber(), e.getMessage());
        }

        return ResponseEntity.ok(new VerifyResponse(
            true,
            order.getOrderNumber(),
            "COD order confirmed. Pay upon parcel delivery."
        ));
    }

    // ─── Cryptographic Signature Verification Helpers ─────────────────────────

    private boolean verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId,
                                             String signature) {
        if (signature == null || razorpayKeySecret == null) {
            return false;
        }
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(
                hexString.toString().getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyWebhookSignature(String rawBody, String signature, String secret) {
        if (signature == null || secret == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(
                hexString.toString().getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean isRazorpayConfigured() {
        return razorpayKeyId != null && !razorpayKeyId.isBlank()
            && !razorpayKeyId.contains("placeholder")
            && razorpayKeySecret != null && !razorpayKeySecret.isBlank()
            && !razorpayKeySecret.contains("placeholder");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    // ─── DTO Records ─────────────────────────────────────────────────────────

    public record CreateOrderRequest(@NotNull Long orderId) {}

    public record CreateOrderResponse(
        String razorpayOrderId,
        long amountPaisa,
        String currency,
        String keyId
    ) {}

    public record VerifyRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
    ) {}

    public record VerifyResponse(boolean success, String orderNumber, String message) {}

    public record CodConfirmRequest(@NotNull Long orderId) {}
}
