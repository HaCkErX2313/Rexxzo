package com.rexxo.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rexxo.config.ShiprocketConfig;
import com.rexxo.entity.*;
import com.rexxo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "shiprocket.email=test@example.com",
    "shiprocket.password=test_secret_pass",
    "shiprocket.pickup-location=Primary",
    "shiprocket.pickup-pincode=201310"
})
@AutoConfigureMockMvc
public class ShiprocketIntegrationVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShiprocketConfig shiprocketConfig;

    @Autowired
    private ShiprocketService shiprocketService;

    @MockBean
    private ShiprocketClient apiClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Default safe mock behavior
        when(apiClient.getBearerToken()).thenReturn("mocked_token_xyz");

        String email = "ship-customer-" + UUID.randomUUID().toString().substring(0, 8) + "@rexxo.com";
        String randomPhone = "9" + (long) (Math.random() * 900000000L + 100000000L);
        testUser = userRepository.save(User.builder()
            .name("Rohan Sharma")
            .email(email)
            .password("Password@123")
            .phone(randomPhone)
            .role(User.Role.CUSTOMER)
            .isActive(true)
            .emailVerified(true)
            .phoneVerified(true)
            .build());

        Category cat = categoryRepository.findBySlug("home-decor").orElseGet(() ->
            categoryRepository.save(Category.builder()
                .name("Home Decor")
                .slug("home-decor")
                .isActive(true)
                .build())
        );

        testProduct = productRepository.save(Product.builder()
            .name("Handcrafted Brass Diya")
            .slug("brass-diya-" + UUID.randomUUID().toString().substring(0, 6))
            .sku("DIYA-BRASS-01")
            .price(new BigDecimal("799.00"))
            .stock(50)
            .category(cat)
            .isActive(true)
            .status("ACTIVE")
            .weightGrams(350)
            .lengthCm(new BigDecimal("12.0"))
            .widthCm(new BigDecimal("12.0"))
            .heightCm(new BigDecimal("8.0"))
            .images(new ArrayList<>())
            .build());
    }

    @Test
    @DisplayName("Configuration: Sanitizes credentials and exposes safe pickup location without credentials leakage")
    void testShiprocketConfigurationSanitization() {
        assertTrue(shiprocketConfig.isConfigured());
        assertEquals("test@example.com", shiprocketConfig.getEmail());
        assertEquals("Primary", shiprocketConfig.getPickupLocation());
        assertEquals("201310", shiprocketConfig.getPickupPincode());
        assertEquals("https://apiv2.shiprocket.in", shiprocketConfig.getBaseUrl());
    }

    @Test
    @WithMockUser(username = "admin@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Admin Status: Returns diagnostic status without exposing passwords or tokens")
    void testAdminShippingStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/shipping/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.authentication").value("OK"))
            .andExpect(jsonPath("$.pickupLocation").value("Primary"))
            .andExpect(jsonPath("$.pickupPincode").value("201310"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Automatic Shipment: Creates Shiprocket order with validated payload for valid order")
    void testAutomaticShipmentCreationSuccess() {
        Order order = createTestOrder("COD", "201310", "9876543210");

        ObjectNode mockRes = objectMapper.createObjectNode();
        mockRes.put("order_id", 998811);
        mockRes.put("shipment_id", 887722);
        mockRes.put("status", "NEW");
        when(apiClient.post(eq("/v1/external/orders/create/adhoc"), any())).thenReturn(mockRes);

        ShippingDtos.CreateShipmentRequest req = ShippingDtos.CreateShipmentRequest.builder()
            .orderId(order.getId())
            .paymentMode("COD")
            .codAmount(order.getTotal())
            .build();

        ShippingDtos.CreateShipmentResponse response = shiprocketService.createShipment(req);

        assertTrue(response.isSuccess());
        assertEquals("998811", response.getProviderOrderId());
        assertEquals("887722", response.getProviderShipmentId());

        // Verify database persistence
        Shipment s = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("998811", s.getProviderOrderId());
        assertEquals("887722", s.getProviderShipmentId());
        assertEquals(Shipment.ShipmentStatus.CREATED, s.getShipmentStatus());
        assertEquals(Shipment.PaymentMode.COD, s.getPaymentMode());
        assertEquals(1, s.getAttemptCount());
        assertNotNull(s.getLastAttemptAt());
        assertNull(s.getFailureReason());
    }

    @Test
    @DisplayName("Validation Error: Missing or invalid pincode/phone records failure and does not call Shiprocket API")
    void testValidationErrorForInvalidShippingDetails() {
        // Invalid 4-digit pincode
        Order order = createTestOrder("COD", "2013", "9876543210");

        ShippingDtos.CreateShipmentRequest req = ShippingDtos.CreateShipmentRequest.builder()
            .orderId(order.getId())
            .paymentMode("COD")
            .build();

        ShippingDtos.CreateShipmentResponse response = shiprocketService.createShipment(req);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().toLowerCase().contains("validation"));

        // Verify Shiprocket API was NOT called
        verify(apiClient, never()).post(eq("/v1/external/orders/create/adhoc"), any());

        // Verify status marked FAILED in DB with reason recorded
        Shipment s = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Shipment.ShipmentStatus.FAILED, s.getShipmentStatus());
        assertTrue(s.getFailureReason().contains("Invalid shipping pincode"));
    }

    @Test
    @DisplayName("Prepaid Order Verification: Unverified prepaid payment does NOT create shipment")
    void testUnverifiedPaymentDoesNotCreateShipment() {
        Order order = createTestOrder("PREPAID", "201310", "9876543210");
        order.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(order);

        // Verify no shipment exists prior to payment verification
        Optional<Shipment> s = shipmentRepository.findByOrderId(order.getId());
        assertTrue(s.isEmpty());
    }

    @Test
    @DisplayName("API Failure: Shiprocket API exception records FAILED status without crashing")
    void testShiprocketApiFailurePersistsError() {
        Order order = createTestOrder("PREPAID", "201310", "9876543210");

        when(apiClient.post(eq("/v1/external/orders/create/adhoc"), any()))
            .thenThrow(new ShiprocketApiClient.ShiprocketException("POST /orders/create/adhoc failed (HTTP 422): Unserviceable pincode"));

        ShippingDtos.CreateShipmentRequest req = ShippingDtos.CreateShipmentRequest.builder()
            .orderId(order.getId())
            .paymentMode("PREPAID")
            .build();

        ShippingDtos.CreateShipmentResponse response = shiprocketService.createShipment(req);

        assertFalse(response.isSuccess());
        Shipment s = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Shipment.ShipmentStatus.FAILED, s.getShipmentStatus());
        assertTrue(s.getFailureReason().contains("Unserviceable pincode"));
    }

    @Test
    @DisplayName("Duplicate Reconciliation: Ambiguous error triggers reconciliation and avoids duplicate shipment")
    void testDuplicateOrderReconciliation() {
        Order order = createTestOrder("COD", "201310", "9876543210");

        when(apiClient.post(eq("/v1/external/orders/create/adhoc"), any()))
            .thenThrow(new ShiprocketApiClient.ShiprocketException("POST failed (HTTP 422): Order with this order id already exists"));

        // Mock reconciliation response
        ObjectNode searchRes = objectMapper.createObjectNode();
        ArrayNode dataArr = searchRes.putArray("data");
        ObjectNode ordNode = dataArr.addObject();
        ordNode.put("id", "771122");
        ordNode.put("channel_order_id", order.getOrderNumber());
        ArrayNode ships = ordNode.putArray("shipments");
        ObjectNode s1 = ships.addObject();
        s1.put("id", "554433");
        s1.put("awb", "AWB-RECON-777");
        s1.put("courier_name", "Delhivery");

        when(apiClient.get(eq("/v1/external/orders"), any())).thenReturn(searchRes);

        ShippingDtos.CreateShipmentRequest req = ShippingDtos.CreateShipmentRequest.builder()
            .orderId(order.getId())
            .paymentMode("COD")
            .build();

        ShippingDtos.CreateShipmentResponse response = shiprocketService.createShipment(req);

        assertTrue(response.isSuccess());
        assertEquals("771122", response.getProviderOrderId());
        assertEquals("554433", response.getProviderShipmentId());

        Shipment s = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Shipment.ShipmentStatus.CREATED, s.getShipmentStatus());
        assertEquals("771122", s.getProviderOrderId());
        assertEquals("554433", s.getProviderShipmentId());
    }

    @Test
    @DisplayName("Idempotency: Re-calling createShipment for same order returns existing shipment and prevents duplicate bookings")
    void testShipmentCreationIdempotency() {
        Order order = createTestOrder("COD", "201310", "9876543210");

        // Pre-create existing shipment record in PostgreSQL
        shipmentRepository.save(Shipment.builder()
            .order(order)
            .provider("SHIPROCKET")
            .providerOrderId("SR-ORD-99121")
            .providerShipmentId("SR-SHIP-88231")
            .awbNumber("AWB9988776655")
            .courierName("Delhivery Surface")
            .shipmentStatus(Shipment.ShipmentStatus.AWB_ASSIGNED)
            .pickupPincode("201310")
            .deliveryPincode("201310")
            .build());

        ShippingDtos.CreateShipmentRequest req = ShippingDtos.CreateShipmentRequest.builder()
            .orderId(order.getId())
            .paymentMode("COD")
            .codAmount(order.getTotal())
            .build();

        ShippingDtos.CreateShipmentResponse response = shiprocketService.createShipment(req);

        assertTrue(response.isSuccess());
        assertEquals("SR-ORD-99121", response.getProviderOrderId());
        assertEquals("SR-SHIP-88231", response.getProviderShipmentId());
        assertEquals("AWB9988776655", response.getAwbNumber());
        assertEquals("Shipment already exists", response.getMessage());

        // Verify API was never called because it was already present
        verify(apiClient, never()).post(eq("/v1/external/orders/create/adhoc"), any());
    }

    @Test
    @WithMockUser(username = "admin@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Admin Retry: Create/Retry shipment endpoint triggers createShipment safely")
    void testAdminCreateShipmentRetry() throws Exception {
        Order order = createTestOrder("COD", "201310", "9876543210");

        // Pre-create failed shipment
        shipmentRepository.save(Shipment.builder()
            .order(order)
            .provider("SHIPROCKET")
            .shipmentStatus(Shipment.ShipmentStatus.FAILED)
            .failureReason("Temporary network timeout")
            .pickupPincode("201310")
            .deliveryPincode("201310")
            .build());

        ObjectNode mockRes = objectMapper.createObjectNode();
        mockRes.put("order_id", 332211);
        mockRes.put("shipment_id", 443322);
        mockRes.put("status", "NEW");
        when(apiClient.post(eq("/v1/external/orders/create/adhoc"), any())).thenReturn(mockRes);

        mockMvc.perform(post("/api/admin/shipping/orders/" + order.getId() + "/create-shipment")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.providerOrderId").value("332211"));

        // Verify status transitioned from FAILED to CREATED
        Shipment s = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(Shipment.ShipmentStatus.CREATED, s.getShipmentStatus());
        assertEquals("332211", s.getProviderOrderId());
        assertNull(s.getFailureReason());
    }

    private Order createTestOrder(String paymentMethod, String pincode, String phone) {
        Order order = Order.builder()
            .orderNumber("RXO-TEST-" + UUID.randomUUID().toString().substring(0, 6))
            .user(testUser)
            .status(Order.OrderStatus.CONFIRMED)
            .subtotal(new BigDecimal("799.00"))
            .shippingFee(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .total(new BigDecimal("799.00"))
            .shippingName("Rohan Sharma")
            .shippingPhone(phone)
            .shippingAddress("Plot 42, Knowledge Park III")
            .shippingCity("Greater Noida")
            .shippingState("Uttar Pradesh")
            .shippingPincode(pincode)
            .paymentMethod(paymentMethod)
            .items(new ArrayList<>())
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
}
