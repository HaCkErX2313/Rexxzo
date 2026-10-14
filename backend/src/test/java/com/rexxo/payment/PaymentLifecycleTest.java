package com.rexxo.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.controller.PaymentController.*;
import com.rexxo.entity.Cart;
import com.rexxo.entity.CartItem;
import com.rexxo.entity.Category;
import com.rexxo.entity.Order;
import com.rexxo.entity.OrderItem;
import com.rexxo.entity.Payment;
import com.rexxo.entity.Product;
import com.rexxo.entity.User;
import com.rexxo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "razorpay.key.id=rzp_test_placeholder",
    "razorpay.key.secret=placeholder_secret",
    "razorpay.webhook.secret=test_webhook_secret_123"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook.secret}")
    private String razorpayWebhookSecret;

    private User customerA;
    private User customerB;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.findBySlug("home-decor").orElseGet(() -> {
            Category c = Category.builder()
                .name("Home Decor")
                .slug("home-decor")
                .isActive(true)
                .build();
            return categoryRepository.save(c);
        });

        customerA = userRepository.findByEmail("customer-a@rexxo.com").orElseGet(() -> {
            User u = User.builder()
                .name("Customer A")
                .email("customer-a@rexxo.com")
                .phone("+919000000001")
                .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
            return userRepository.save(u);
        });

        customerB = userRepository.findByEmail("customer-b@rexxo.com").orElseGet(() -> {
            User u = User.builder()
                .name("Customer B")
                .email("customer-b@rexxo.com")
                .phone("+919000000002")
                .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
            return userRepository.save(u);
        });

        testProduct = productRepository.findBySlug("test-brass-lamp").orElseGet(() -> {
            Product p = Product.builder()
                .name("Test Brass Lamp")
                .slug("test-brass-lamp")
                .sku("SKU-LAMP-001")
                .price(new BigDecimal("1200.00"))
                .stock(50)
                .category(testCategory)
                .isActive(true)
                .status("ACTIVE")
                .images(new ArrayList<>())
                .build();
            return productRepository.save(p);
        });
    }

    private Order createTestOrder(User user, BigDecimal amount, Order.OrderStatus status) {
        String orderNumber = "RXO-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
            .orderNumber(orderNumber)
            .user(user)
            .status(status)
            .subtotal(amount)
            .shippingFee(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .total(amount)
            .shippingName(user.getName())
            .shippingPhone(user.getPhone())
            .shippingAddress("123 Test Street")
            .shippingCity("New Delhi")
            .shippingState("Delhi")
            .shippingPincode("110001")
            .paymentMethod("PREPAID")
            .build();

        OrderItem item = OrderItem.builder()
            .order(order)
            .product(testProduct)
            .productName(testProduct.getName())
            .unitPrice(testProduct.getPrice())
            .quantity(1)
            .subtotal(testProduct.getPrice())
            .build();
        order.getItems().add(item);

        return orderRepository.save(order);
    }

    private String calculateHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @WithMockUser(username = "customer-a@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Create Razorpay Order: Authorized customer creates order and gets valid paise amount and order ID")
    void testCreateRazorpayOrderSuccess() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING_PAYMENT);

        CreateOrderRequest req = new CreateOrderRequest(order.getId());

        mockMvc.perform(post("/api/payments/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.amountPaisa").value(120000))
                .andExpect(jsonPath("$.razorpayOrderId").isNotEmpty());

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        assertNotNull(savedPayment, "Payment record must be persisted in PostgreSQL");
        assertEquals(Payment.PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals("INR", savedPayment.getCurrency());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @WithMockUser(username = "customer-a@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Idempotency: Re-requesting create-order returns the existing razorpayOrderId")
    void testCreateRazorpayOrderIdempotency() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING_PAYMENT);

        CreateOrderRequest req = new CreateOrderRequest(order.getId());

        String firstResponse = mockMvc.perform(post("/api/payments/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/payments/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CreateOrderResponse res1 = objectMapper.readValue(firstResponse, CreateOrderResponse.class);
        CreateOrderResponse res2 = objectMapper.readValue(secondResponse, CreateOrderResponse.class);

        assertEquals(res1.razorpayOrderId(), res2.razorpayOrderId(), "Razorpay order ID must be reused idempotently");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @WithMockUser(username = "customer-b@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Security: Customer B cannot create Razorpay order for Customer A's order")
    void testOtherCustomerCannotCreateOrder() throws Exception {
        Order orderA = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING_PAYMENT);

        CreateOrderRequest req = new CreateOrderRequest(orderA.getId());

        mockMvc.perform(post("/api/payments/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @WithMockUser(username = "customer-a@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Verify Payment: Valid cryptographic signature marks order as PAID and clears customer cart")
    void testVerifyPaymentValidSignature() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING_PAYMENT);
        String rzpOrderId = "order_test_" + UUID.randomUUID().toString().substring(0, 8);
        String rzpPaymentId = "pay_test_" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .currency("INR")
            .razorpayOrderId(rzpOrderId)
            .status(Payment.PaymentStatus.PENDING)
            .build();
        paymentRepository.save(payment);

        // Put an item in customerA's cart to verify cart is cleared
        Cart cart = cartRepository.findByUserId(customerA.getId()).orElseGet(() -> {
            Cart c = Cart.builder().user(customerA).build();
            return cartRepository.save(c);
        });
        cartItemRepository.deleteByCartId(cart.getId());
        CartItem cartItem = CartItem.builder()
            .cart(cart)
            .product(testProduct)
            .quantity(1)
            .unitPrice(testProduct.getPrice())
            .build();
        cartItemRepository.save(cartItem);

        // Generate valid HMAC-SHA256 signature
        String validSignature = calculateHmac(rzpOrderId + "|" + rzpPaymentId, razorpayKeySecret);

        VerifyRequest req = new VerifyRequest(rzpOrderId, rzpPaymentId, validSignature);

        mockMvc.perform(post("/api/payments/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderNumber").value(order.getOrderNumber()));

        // Check Database State
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.PAID, updatedOrder.getStatus());

        Payment updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.PAID, updatedPayment.getStatus());
        assertEquals(rzpPaymentId, updatedPayment.getRazorpayPaymentId());

        // Check Cart is cleared
        assertFalse(cartItemRepository.existsById(cartItem.getId()), "Cart must be cleared after verified payment");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @WithMockUser(username = "customer-a@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Verify Payment: Invalid/tampered signature is rejected and marks payment FAILED")
    void testVerifyPaymentInvalidSignatureFails() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING_PAYMENT);
        String rzpOrderId = "order_test_" + UUID.randomUUID().toString().substring(0, 8);
        String rzpPaymentId = "pay_test_" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .currency("INR")
            .razorpayOrderId(rzpOrderId)
            .status(Payment.PaymentStatus.PENDING)
            .build();
        paymentRepository.save(payment);

        VerifyRequest req = new VerifyRequest(rzpOrderId, rzpPaymentId, "invalid_tampered_signature_hex");

        mockMvc.perform(post("/api/payments/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid signature")));

        // Order remains PENDING_PAYMENT, payment marked FAILED
        Payment failedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.FAILED, failedPayment.getStatus());

        Order unchangedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.PENDING_PAYMENT, unchangedOrder.getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @WithMockUser(username = "customer-a@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Idempotency: Duplicate verify request returns success without double-processing")
    void testDuplicateVerifyPaymentIsIdempotent() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PAID);
        String rzpOrderId = "order_test_" + UUID.randomUUID().toString().substring(0, 8);
        String rzpPaymentId = "pay_test_" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .currency("INR")
            .razorpayOrderId(rzpOrderId)
            .razorpayPaymentId(rzpPaymentId)
            .status(Payment.PaymentStatus.PAID)
            .build();
        paymentRepository.save(payment);

        VerifyRequest req = new VerifyRequest(rzpOrderId, rzpPaymentId, "any_sig_since_already_paid");

        mockMvc.perform(post("/api/payments/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already verified")));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Webhook: Valid payment.captured webhook marks order as PAID")
    void testWebhookPaymentCapturedSuccess() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING_PAYMENT);
        String rzpOrderId = "order_wh_" + UUID.randomUUID().toString().substring(0, 8);
        String rzpPaymentId = "pay_wh_" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .currency("INR")
            .razorpayOrderId(rzpOrderId)
            .status(Payment.PaymentStatus.PENDING)
            .build();
        paymentRepository.save(payment);

        String webhookPayload = """
            {
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "%s",
                    "order_id": "%s",
                    "status": "captured",
                    "amount": 120000,
                    "currency": "INR"
                  }
                }
              }
            }
            """.formatted(rzpPaymentId, rzpOrderId);

        String webhookSignature = calculateHmac(webhookPayload, razorpayWebhookSecret);

        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Razorpay-Signature", webhookSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processed"));

        Order paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.PAID, paidOrder.getStatus());
        assertEquals("PREPAID", paidOrder.getPaymentMethod());

        Payment paidPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.PAID, paidPayment.getStatus());
        assertEquals(rzpPaymentId, paidPayment.getRazorpayPaymentId());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Webhook: Invalid webhook signature is rejected with HTTP 400")
    void testWebhookInvalidSignatureRejected() throws Exception {
        String webhookPayload = "{\"event\": \"payment.captured\"}";

        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Razorpay-Signature", "wrong_signature_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("Webhook Safety: Late payment.failed webhook does not overwrite an already PAID order")
    void testLateFailureWebhookDoesNotOverwritePaidOrder() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PAID);
        String rzpOrderId = "order_wh_late_" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotal())
            .currency("INR")
            .razorpayOrderId(rzpOrderId)
            .status(Payment.PaymentStatus.PAID)
            .build();
        paymentRepository.save(payment);

        String failedWebhookPayload = """
            {
              "event": "payment.failed",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_late_fail",
                    "order_id": "%s",
                    "status": "failed",
                    "error_description": "Late bank failure"
                  }
                }
              }
            }
            """.formatted(rzpOrderId);

        String signature = calculateHmac(failedWebhookPayload, razorpayWebhookSecret);

        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(failedWebhookPayload))
                .andExpect(status().isOk());

        // Order and Payment MUST remain PAID
        Order checkOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.PAID, checkOrder.getStatus(), "Paid order must NOT be overwritten by late failure");

        Payment checkPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.PAID, checkPayment.getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @WithMockUser(username = "customer-a@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("COD Flow: Confirm COD order marks order CONFIRMED and clears cart")
    void testCodConfirmationSuccess() throws Exception {
        Order order = createTestOrder(customerA, new BigDecimal("1200.00"), Order.OrderStatus.PENDING);

        Cart cart = cartRepository.findByUserId(customerA.getId()).orElseGet(() -> {
            Cart c = Cart.builder().user(customerA).build();
            return cartRepository.save(c);
        });
        cartItemRepository.deleteByCartId(cart.getId());
        CartItem cartItem = CartItem.builder()
            .cart(cart)
            .product(testProduct)
            .quantity(1)
            .unitPrice(testProduct.getPrice())
            .build();
        cartItemRepository.save(cartItem);

        CodConfirmRequest req = new CodConfirmRequest(order.getId());

        mockMvc.perform(post("/api/payments/confirm-cod")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderNumber").value(order.getOrderNumber()));

        Order codOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.CONFIRMED, codOrder.getStatus());
        assertEquals("COD", codOrder.getPaymentMethod());

        // Cart is cleared
        assertFalse(cartItemRepository.existsById(cartItem.getId()));
    }
}
