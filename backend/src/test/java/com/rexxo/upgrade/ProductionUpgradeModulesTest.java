package com.rexxo.upgrade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.controller.ReviewController.ReviewRequest;
import com.rexxo.entity.*;
import com.rexxo.repository.*;
import com.rexxo.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class ProductionUpgradeModulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final String CUSTOMER_EMAIL = "upgradetestcust@rexxo.com";
    private static final String OTHER_CUSTOMER_EMAIL = "othercustomer@rexxo.com";
    private static final String ADMIN_EMAIL = "upgradetestadmin@rexxo.com";

    private User customer;
    private User otherCustomer;
    private User admin;
    private Product product;
    private Order deliveredOrder;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        try {
            jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check");
            jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check");
        } catch (Exception ignored) {}

        customer = userRepository.findByEmail(CUSTOMER_EMAIL).orElseGet(() ->
            userRepository.save(User.builder()
                .name("Upgrade Test Customer")
                .email(CUSTOMER_EMAIL)
                .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                .phone("+919111000101")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build())
        );

        otherCustomer = userRepository.findByEmail(OTHER_CUSTOMER_EMAIL).orElseGet(() ->
            userRepository.save(User.builder()
                .name("Other Customer")
                .email(OTHER_CUSTOMER_EMAIL)
                .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                .phone("+919111000102")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build())
        );

        admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() ->
            userRepository.save(User.builder()
                .name("Upgrade Test Admin")
                .email(ADMIN_EMAIL)
                .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                .phone("+919111000103")
                .role(User.Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build())
        );

        Category cat = categoryRepository.findBySlug("upgrade-cat").orElseGet(() ->
            categoryRepository.save(Category.builder().name("Upgrade Category").slug("upgrade-cat").build())
        );

        product = productRepository.findBySlug("upgrade-product").orElseGet(() ->
            productRepository.save(Product.builder()
                .name("Upgrade Minimalist Lamp")
                .slug("upgrade-product")
                .sku("UPG-LMP-01")
                .price(new BigDecimal("2999.00"))
                .stock(20)
                .category(cat)
                .isActive(true)
                .build())
        );

        // Delivered order for testing return and verified reviews
        deliveredOrder = orderRepository.findByOrderNumber("RXO-UPG-DELIVERED").orElseGet(() -> {
            Order o = Order.builder()
                .orderNumber("RXO-UPG-DELIVERED")
                .user(customer)
                .status(Order.OrderStatus.DELIVERED)
                .subtotal(new BigDecimal("5998.00"))
                .shippingFee(BigDecimal.ZERO)
                .total(new BigDecimal("5998.00"))
                .shippingName("Test Cust")
                .shippingPhone("+919111000101")
                .shippingAddress("123 Test Street")
                .shippingCity("New Delhi")
                .shippingState("Delhi")
                .shippingPincode("110001")
                .build();
            OrderItem item = OrderItem.builder()
                .order(o)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(2)
                .subtotal(new BigDecimal("5998.00"))
                .build();
            o.getItems().add(item);
            return orderRepository.save(o);
        });

        // Pending order for cancellation test
        pendingOrder = orderRepository.findByOrderNumber("RXO-UPG-PENDING").orElseGet(() -> {
            Order o = Order.builder()
                .orderNumber("RXO-UPG-PENDING")
                .user(customer)
                .status(Order.OrderStatus.PENDING)
                .subtotal(new BigDecimal("2999.00"))
                .shippingFee(new BigDecimal("49.00"))
                .total(new BigDecimal("3048.00"))
                .shippingName("Test Cust")
                .shippingPhone("+919111000101")
                .shippingAddress("123 Test Street")
                .shippingCity("New Delhi")
                .shippingState("Delhi")
                .shippingPincode("110001")
                .build();
            OrderItem item = OrderItem.builder()
                .order(o)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(1)
                .subtotal(new BigDecimal("2999.00"))
                .build();
            o.getItems().add(item);
            return orderRepository.save(o);
        });
    }

    // ─── 1. Security Tests ───────────────────────────────────────────────────
    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Security: Regular customer must be forbidden from accessing admin endpoints")
    void testCustomerForbiddenFromAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("Admin: Admin with ROLE_ADMIN can access dashboard and analytics")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").exists());

        mockMvc.perform(get("/api/admin/analytics?range=30d"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.range").value("30d"))
            .andExpect(jsonPath("$.totalRevenue").exists());
    }

    // ─── 2. Returns & Refunds Lifecycle ──────────────────────────────────────
    @Test
    @WithMockUser(username = OTHER_CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Returns: Customer cannot request return for another customer's order")
    void testCannotReturnOtherCustomerOrder() throws Exception {
        Long orderItemId = deliveredOrder.getItems().get(0).getId();
        var req = new ReturnService.CreateReturnRequestDto(
            deliveredOrder.getId(), "DEFECTIVE", "Item arrived with broken base",
            List.of(new ReturnService.ReturnItemDto(orderItemId, 1))
        );

        mockMvc.perform(post("/api/returns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Returns: Customer cannot exceed purchased quantity")
    void testCannotExceedPurchasedQuantity() throws Exception {
        Long orderItemId = deliveredOrder.getItems().get(0).getId();
        // purchased quantity is 2; try returning 3
        var req = new ReturnService.CreateReturnRequestDto(
            deliveredOrder.getId(), "DEFECTIVE", "Too many items",
            List.of(new ReturnService.ReturnItemDto(orderItemId, 3))
        );

        mockMvc.perform(post("/api/returns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Returns: Verified customer requests return, views status, and admin approves refund")
    void testCompleteReturnAndRefundWorkflow() throws Exception {
        Long orderItemId = deliveredOrder.getItems().get(0).getId();
        var req = new ReturnService.CreateReturnRequestDto(
            deliveredOrder.getId(), "DEFECTIVE", "Defective piece",
            List.of(new ReturnService.ReturnItemDto(orderItemId, 1))
        );

        // 1. Submit return request
        MvcResult res = mockMvc.perform(post("/api/returns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETURN_REQUESTED"))
            .andExpect(jsonPath("$.refundAmount").value(2999.00))
            .andReturn();

        Long returnId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // 2. Customer views own returns
        mockMvc.perform(get("/api/returns/my"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(returnId));

        // 3. Admin views and approves return
        mockMvc.perform(patch("/api/admin/returns/" + returnId + "/approve")
                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("adminNotes", "Approved for return pickup"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETURN_APPROVED"));

        // 4. Admin schedules pickup
        mockMvc.perform(patch("/api/admin/returns/" + returnId + "/pickup")
                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("courier", "Shiprocket Return Partner", "awb", "AWB-RET-123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PICKUP_SCHEDULED"));

        // 5. Admin marks received
        mockMvc.perform(patch("/api/admin/returns/" + returnId + "/receive")
                .with(user(ADMIN_EMAIL).roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECEIVED"));

        // 6. Admin approves refund -> Status becomes REFUND_PENDING (Razorpay ready, no fake payment)
        mockMvc.perform(patch("/api/admin/returns/" + returnId + "/refund")
                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "Quality check passed"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REFUND_PENDING"))
            .andExpect(jsonPath("$.refund.status").value("REFUND_PENDING"))
            .andExpect(jsonPath("$.refund.amount").value(2999.00));
    }

    // ─── 3. Reviews & Ratings ────────────────────────────────────────────────
    @Test
    @WithMockUser(username = OTHER_CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Reviews: Non-purchaser cannot review product")
    void testNonPurchaserCannotReview() throws Exception {
        ReviewRequest req = new ReviewRequest(5, "Looks great", "I did not buy it though");

        mockMvc.perform(post("/api/reviews/product/" + product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Reviews: Verified purchaser can review, verified purchase badge set, stats recalculated")
    void testVerifiedPurchaserReviewWorkflow() throws Exception {
        ReviewRequest req = new ReviewRequest(5, "Exceptional craftsmanship", "Stunning minimal aesthetic and material quality.");

        // 1. Submit review
        mockMvc.perform(post("/api/reviews/product/" + product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.verifiedPurchase").value(true));

        // 2. Fetch rating summary
        mockMvc.perform(get("/api/reviews/product/" + product.getId() + "/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalReviews").value(1))
            .andExpect(jsonPath("$.averageRating").value(5.0))
            .andExpect(jsonPath("$.ratingDistribution.5").value(1))
            .andExpect(jsonPath("$.alreadyReviewed").value(true));

        // 3. Duplicate review rejected
        mockMvc.perform(post("/api/reviews/product/" + product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    // ─── 4. Order Cancellation ───────────────────────────────────────────────
    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @DisplayName("Orders: Customer can cancel pending order and restock inventory")
    void testCustomerCancelPendingOrder() throws Exception {
        int initialStock = product.getStock();

        mockMvc.perform(patch("/api/orders/" + pendingOrder.getId() + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Order successfully cancelled and items restocked"));

        // Verify status and restocked inventory
        Order updated = orderRepository.findById(pendingOrder.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Order.OrderStatus.CANCELLED, updated.getStatus());

        Product restocked = productRepository.findById(product.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(initialStock + 1, restocked.getStock());
    }
}
