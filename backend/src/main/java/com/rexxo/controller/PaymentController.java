package com.rexxo.controller;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import com.rexxo.shipping.ShippingDtos;
import com.rexxo.shipping.ShiprocketService;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handles Razorpay payment lifecycle:
 * 1. Create Razorpay order (returns order_id to frontend)
 * 2. Verify signature (server-side) after frontend reports payment
 * 3. On success: mark order PAID and trigger Shiprocket shipment creation
 *
 * CRITICAL: Payment is only marked successful after cryptographic
 * signature verification on the backend. Frontend payment success
 * reports are NEVER trusted directly.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ShiprocketService shiprocketService;

    // ─── Create Razorpay Order ────────────────────────────────────────────────

    /**
     * POST /api/payments/create-order
     * Creates a Razorpay order for the given REXXO order.
     * Frontend uses the returned razorpayOrderId to open the Razorpay checkout modal.
     */
    @PostMapping("/create-order")
    @Transactional
    public ResponseEntity<CreateOrderResponse> createRazorpayOrder(
            Authentication auth,
            @Valid @RequestBody CreateOrderRequest request) {

        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(request.orderId(), user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        if (order.getStatus() != Order.OrderStatus.PENDING
            && order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw RexxoException.badRequest("Order is not in a payable state: " + order.getStatus());
        }

        // Check if Razorpay order already created (idempotency)
        Payment existingPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getRazorpayOrderId() != null) {
            return ResponseEntity.ok(new CreateOrderResponse(
                existingPayment.getRazorpayOrderId(),
                order.getTotal().multiply(BigDecimal.valueOf(100)).longValue(),
                "INR",
                razorpayKeyId
            ));
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            long amountPaisa = order.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaisa);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderNumber());

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Create or update Payment record
            Payment payment = existingPayment != null ? existingPayment :
                Payment.builder().order(order).build();
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setAmount(order.getTotal());
            payment.setStatus(Payment.PaymentStatus.PENDING);
            paymentRepository.save(payment);

            return ResponseEntity.ok(new CreateOrderResponse(
                razorpayOrderId, amountPaisa, "INR", razorpayKeyId));

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for {}: {}", order.getOrderNumber(), e.getMessage());
            throw RexxoException.badRequest("Payment initialization failed: " + e.getMessage());
        }
    }

    // ─── Verify Payment ───────────────────────────────────────────────────────

    /**
     * POST /api/payments/verify
     * Verifies the Razorpay payment signature cryptographically.
     * This is the ONLY way a REXXO order gets marked PAID.
     * After verification, triggers Shiprocket shipment creation.
     */
    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<VerifyResponse> verifyPayment(
            Authentication auth,
            @Valid @RequestBody VerifyRequest request) {

        User user = getUser(auth);

        // 1. Load the REXXO order via the Razorpay order ID
        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
            .orElseThrow(() -> RexxoException.notFound("Payment record not found"));

        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("Order does not belong to this user");
        }

        // 2. Verify Razorpay signature (HMAC-SHA256)
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
            throw RexxoException.badRequest("Payment verification failed. Please contact support.");
        }

        // 3. Mark payment and order as PAID
        log.info("Payment verified for order {}", order.getOrderNumber());
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setStatus(Payment.PaymentStatus.PAID);
        paymentRepository.save(payment);

        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentMethod("PREPAID");
        orderRepository.save(order);

        // 4. Trigger Shiprocket shipment creation asynchronously
        // We do this in a best-effort manner — payment success is NEVER rolled back
        // if Shiprocket fails. Shipment goes to CREATION_PENDING for admin retry.
        try {
            ShippingDtos.CreateShipmentRequest shipReq = ShippingDtos.CreateShipmentRequest.builder()
                .orderId(order.getId())
                .paymentMode("PREPAID")
                .build();
            shiprocketService.createShipment(shipReq);
        } catch (Exception e) {
            log.error("Shiprocket shipment creation failed after payment for order {}: {}",
                order.getOrderNumber(), e.getMessage());
            // IMPORTANT: Do NOT fail the payment response. Order remains PAID.
            // Admin can retry shipment creation from the admin dashboard.
        }

        return ResponseEntity.ok(new VerifyResponse(
            true,
            order.getOrderNumber(),
            "Payment successful. Your order is confirmed."
        ));
    }

    // ─── COD Order ────────────────────────────────────────────────────────────

    /**
     * POST /api/payments/confirm-cod
     * Confirms a COD order. No payment collected now — courier collects on delivery.
     */
    @PostMapping("/confirm-cod")
    @Transactional
    public ResponseEntity<VerifyResponse> confirmCodOrder(
            Authentication auth,
            @Valid @RequestBody CodConfirmRequest request) {

        User user = getUser(auth);
        Order order = orderRepository.findByIdAndUserId(request.orderId(), user.getId())
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        if (order.getStatus() != Order.OrderStatus.PENDING
            && order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw RexxoException.badRequest("Order is not in a confirmable state");
        }

        // Create COD payment record
        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .method(Payment.PaymentMethod.COD)
            .status(Payment.PaymentStatus.PENDING) // COD: payment pending until delivery
            .build();
        paymentRepository.save(payment);

        // Mark order as CONFIRMED (not PAID — COD payment happens at delivery)
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setPaymentMethod("COD");
        orderRepository.save(order);

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
            // Order remains CONFIRMED — admin can create shipment manually
        }

        return ResponseEntity.ok(new VerifyResponse(
            true,
            order.getOrderNumber(),
            "COD order confirmed. Pay on delivery."
        ));
    }

    // ─── Signature Verification ───────────────────────────────────────────────

    private boolean verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId,
                                             String signature) {
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
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    // ─── Records ─────────────────────────────────────────────────────────────

    record CreateOrderRequest(@NotBlank Long orderId) {}

    record CreateOrderResponse(String razorpayOrderId, long amountPaisa,
                               String currency, String keyId) {}

    record VerifyRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
    ) {}

    record VerifyResponse(boolean success, String orderNumber, String message) {}

    record CodConfirmRequest(@NotBlank Long orderId) {}
}
