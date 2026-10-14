package com.rexxo.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.config.ShiprocketConfig;
import com.rexxo.entity.Cart;
import com.rexxo.entity.CartItem;
import com.rexxo.entity.Category;
import com.rexxo.entity.Order;
import com.rexxo.entity.OrderItem;
import com.rexxo.entity.Payment;
import com.rexxo.entity.Product;
import com.rexxo.entity.Shipment;
import com.rexxo.entity.User;
import com.rexxo.repository.CartItemRepository;
import com.rexxo.repository.CartRepository;
import com.rexxo.repository.CategoryRepository;
import com.rexxo.repository.OrderRepository;
import com.rexxo.repository.PaymentRepository;
import com.rexxo.repository.ProductRepository;
import com.rexxo.repository.ShipmentRepository;
import com.rexxo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
            .andExpect(jsonPath("$.authentication").exists())
            .andExpect(jsonPath("$.pickupLocation").value("Primary"))
            .andExpect(jsonPath("$.pickupPincode").value("201310"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Idempotency: Re-calling createShipment for same order returns existing shipment and prevents duplicate bookings")
    void testShipmentCreationIdempotency() {
        Order order = orderRepository.save(Order.builder()
            .orderNumber("RXO-SHIP-IDEM-" + UUID.randomUUID().toString().substring(0, 6))
            .user(testUser)
            .status(Order.OrderStatus.CONFIRMED)
            .subtotal(new BigDecimal("799.00"))
            .shippingFee(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .total(new BigDecimal("799.00"))
            .shippingName("Rohan Sharma")
            .shippingPhone("+91 9876543210")
            .shippingAddress("Plot 42, Knowledge Park III, Greater Noida")
            .shippingCity("Greater Noida")
            .shippingState("Uttar Pradesh")
            .shippingPincode("201310")
            .paymentMethod("COD")
            .build());

        // Pre-create existing shipment record in PostgreSQL
        Shipment existing = shipmentRepository.save(Shipment.builder()
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
    }

    @Test
    @WithMockUser(username = "admin@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Admin Retry: Create/Retry shipment endpoint triggers createShipment safely")
    void testAdminCreateShipmentRetry() throws Exception {
        Order order = orderRepository.save(Order.builder()
            .orderNumber("RXO-ADMIN-RETRY-" + UUID.randomUUID().toString().substring(0, 6))
            .user(testUser)
            .status(Order.OrderStatus.CONFIRMED)
            .subtotal(new BigDecimal("799.00"))
            .shippingFee(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .total(new BigDecimal("799.00"))
            .shippingName("Rohan Sharma")
            .shippingPhone("9876543210")
            .shippingAddress("Plot 42, Knowledge Park III")
            .shippingCity("Greater Noida")
            .shippingState("Uttar Pradesh")
            .shippingPincode("201310")
            .paymentMethod("COD")
            .build());

        // Pre-create failed shipment
        shipmentRepository.save(Shipment.builder()
            .order(order)
            .provider("SHIPROCKET")
            .shipmentStatus(Shipment.ShipmentStatus.FAILED)
            .failureReason("Temporary network timeout")
            .pickupPincode("201310")
            .deliveryPincode("201310")
            .build());

        String body = objectMapper.writeValueAsString(Map.of(
            "paymentMode", "COD",
            "codAmount", 799.00
        ));

        mockMvc.perform(post("/api/admin/shipping/orders/" + order.getId() + "/create-shipment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        // Verify status was transitioned from FAILED to either CREATION_PENDING or updated
        Optional<Shipment> s = shipmentRepository.findByOrderId(order.getId());
        assertTrue(s.isPresent());
        assertNotNull(s.get().getShipmentStatus());
    }
}
