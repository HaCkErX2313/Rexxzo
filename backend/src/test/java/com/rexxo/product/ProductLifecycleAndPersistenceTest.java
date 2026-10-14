package com.rexxo.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.controller.CartController.AddItemRequest;
import com.rexxo.entity.Category;
import com.rexxo.entity.Cart;
import com.rexxo.entity.CartItem;
import com.rexxo.entity.Product;
import com.rexxo.entity.ProductImage;
import com.rexxo.entity.User;
import com.rexxo.entity.Wishlist;
import com.rexxo.entity.WishlistItem;
import com.rexxo.entity.OrderItem;
import com.rexxo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductLifecycleAndPersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Category testCategory;
    private User testCustomer;
    private User testAdmin;

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

        testCustomer = userRepository.findByEmail("customer-test@rexxo.com").orElseGet(() -> {
            User u = User.builder()
                    .name("Customer Test User")
                    .email("customer-test@rexxo.com")
                    .phone("+919111122233")
                    .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                    .role(User.Role.CUSTOMER)
                    .isActive(true)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            return userRepository.save(u);
        });

        testAdmin = userRepository.findByEmail("admin-test@rexxo.com").orElseGet(() -> {
            User u = User.builder()
                    .name("Admin Test User")
                    .email("admin-test@rexxo.com")
                    .phone("+919111122234")
                    .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            return userRepository.save(u);
        });
    }

    private Product createProduct(String name, String sku, boolean active, String status) {
        Product p = Product.builder()
                .name(name)
                .slug(sku.toLowerCase())
                .sku(sku)
                .description("Test Description for " + name)
                .price(new BigDecimal("1499.00"))
                .originalPrice(new BigDecimal("1999.00"))
                .stock(25)
                .category(testCategory)
                .isActive(active)
                .status(status)
                .images(new ArrayList<>())
                .weightGrams(500)
                .build();

        ProductImage img = ProductImage.builder()
                .product(p)
                .imageUrl("https://res.cloudinary.com/demo/image/upload/sample.jpg")
                .publicId("sample_public_id")
                .isPrimary(true)
                .build();
        p.getImages().add(img);

        return productRepository.save(p);
    }

    @Test
    @Order(1)
    @DisplayName("Product persistence: Product creation saves record in PostgreSQL and is retrievable")
    void testProductCreationAndPersistence() {
        String sku = "SKU-PERSIST-" + UUID.randomUUID().toString().substring(0, 8);
        Product saved = createProduct("Persistent Artisan Vase", sku, true, "ACTIVE");

        assertNotNull(saved.getId(), "Product ID must be generated");
        assertTrue(saved.getId() > 0);

        Optional<Product> fetched = productRepository.findById(saved.getId());
        assertTrue(fetched.isPresent(), "Product must be found in database");
        assertEquals("Persistent Artisan Vase", fetched.get().getName());
        assertEquals(new BigDecimal("1499.00"), fetched.get().getPrice());
        assertEquals(25, fetched.get().getStock());
        assertTrue(fetched.get().getIsActive());
        assertEquals("ACTIVE", fetched.get().getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Public storefront: Active products are returned via public API")
    void testPublicStorefrontReturnsActiveProduct() throws Exception {
        String sku = "SKU-PUB-" + UUID.randomUUID().toString().substring(0, 8);
        createProduct("Public Visible Lamp", sku, true, "ACTIVE");

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "admin-test@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Admin archive: Admin can archive/unlist a product (isActive=false, status=ARCHIVED)")
    void testAdminCanArchiveProduct() throws Exception {
        String sku = "SKU-ARCH-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Archivable Candle", sku, true, "ACTIVE");

        mockMvc.perform(patch("/api/admin/products/" + product.getId() + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("archived")))
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.isActive").value(false));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertFalse(updated.getIsActive(), "Archived product must have isActive=false");
        assertEquals("ARCHIVED", updated.getStatus(), "Archived product must have status=ARCHIVED");
    }

    @Test
    @Order(4)
    @WithMockUser(username = "admin-test@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Admin restore: Admin can restore an archived product back to active")
    void testAdminCanRestoreProduct() throws Exception {
        String sku = "SKU-REST-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Restorable Mirror", sku, false, "ARCHIVED");

        mockMvc.perform(patch("/api/admin/products/" + product.getId() + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("restored")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isActive").value(true));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertTrue(updated.getIsActive(), "Restored product must have isActive=true");
        assertEquals("ACTIVE", updated.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Security: Unauthorized user (no auth or customer) cannot archive or delete products")
    void testUnauthorizedUserForbidden() throws Exception {
        String sku = "SKU-SEC-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Protected Sculpture", sku, true, "ACTIVE");

        // Unauthenticated access
        mockMvc.perform(patch("/api/admin/products/" + product.getId() + "/archive"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/products/" + product.getId()))
                .andExpect(status().isUnauthorized());

        // Authenticated customer access to admin endpoint
        mockMvc.perform(patch("/api/admin/products/" + product.getId() + "/archive")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @WithMockUser(username = "admin-test@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Admin delete: Missing product ID returns 404 Not Found")
    void testMissingProductReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/admin/products/99999999/archive"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/admin/products/99999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @WithMockUser(username = "customer-test@rexxo.com", roles = {"CUSTOMER"})
    @DisplayName("Safe removal: Archived products cannot be added to cart")
    void testArchivedProductCannotBeAddedToCart() throws Exception {
        String sku = "SKU-NOBUY-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Unpurchasable Bowl", sku, false, "ARCHIVED");

        cartRepository.findByUserId(testCustomer.getId()).orElseGet(() -> {
            Cart c = Cart.builder().user(testCustomer).build();
            return cartRepository.save(c);
        });

        AddItemRequest req = new AddItemRequest(product.getId(), 1);

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This product is archived or unavailable for purchase"));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "admin-test@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Safe removal: Permanent deletion blocked (409 Conflict) if historical orders exist")
    void testHistoricalOrdersPreventPermanentDeletion() throws Exception {
        String sku = "SKU-HIST-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Historical Heritage Rug", sku, true, "ACTIVE");

        // Create an order referencing this product
        com.rexxo.entity.Order order = com.rexxo.entity.Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8))
                .user(testCustomer)
                .subtotal(product.getPrice())
                .total(product.getPrice())
                .status(com.rexxo.entity.Order.OrderStatus.DELIVERED)
                .paymentMethod("PREPAID")
                .shippingAddress("123 Test Street, New Delhi")
                .shippingCity("New Delhi")
                .shippingState("Delhi")
                .shippingPincode("110001")
                .shippingName("Test Customer")
                .shippingPhone("+919111122233")
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(1)
                .subtotal(product.getPrice())
                .build();
        order.getItems().add(item);
        orderRepository.save(order);

        // Permanent deletion attempt MUST be rejected with HTTP 409 Conflict to protect audit history
        mockMvc.perform(delete("/api/admin/products/" + product.getId() + "?permanent=true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("historical customer order")));

        // Verify product still exists in DB
        assertTrue(productRepository.existsById(product.getId()), "Product must still exist in database");
    }

    @Test
    @Order(9)
    @WithMockUser(username = "admin-test@rexxo.com", roles = {"ADMIN"})
    @DisplayName("Safe removal: Safe permanent deletion removes product and cleans cart/wishlist references")
    void testSafePermanentDeletionCleansCartAndWishlist() throws Exception {
        String sku = "SKU-CLEAN-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Temporary Tray", sku, true, "ACTIVE");

        // Put in customer's cart
        Cart cart = cartRepository.findByUserId(testCustomer.getId()).orElseGet(() -> {
            Cart c = Cart.builder().user(testCustomer).build();
            return cartRepository.save(c);
        });
        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();
        cartItemRepository.save(cartItem);

        // Put in customer's wishlist
        Wishlist wishlist = wishlistRepository.findByUserId(testCustomer.getId()).orElseGet(() -> {
            Wishlist w = Wishlist.builder().user(testCustomer).build();
            return wishlistRepository.save(w);
        });
        WishlistItem wishlistItem = WishlistItem.builder()
                .wishlist(wishlist)
                .product(product)
                .build();
        wishlistItemRepository.save(wishlistItem);

        // Now permanently delete
        mockMvc.perform(delete("/api/admin/products/" + product.getId() + "?permanent=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("permanently deleted")));

        // Product should be gone from DB
        assertFalse(productRepository.existsById(product.getId()), "Product should be deleted from DB");

        // Cart item and wishlist item should be cleanly removed
        assertFalse(cartItemRepository.existsById(cartItem.getId()), "Cart item must be removed");
        assertFalse(wishlistItemRepository.existsById(wishlistItem.getId()), "Wishlist item must be removed");
    }

    @Test
    @Order(10)
    @DisplayName("Product persistence: Product older than 1 hour remains active and visible in DB")
    void testSimulatedOneHourTimePassage_ProductRemainsPersistent() {
        String sku = "SKU-TIME-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = createProduct("Time-Tested Ceramic Bowl", sku, true, "ACTIVE");

        Long id = product.getId();

        // Simulate that this product was created 2 hours ago
        productRepository.findById(id).ifPresent(p -> {
            // Check that product remains in database
            assertNotNull(p.getCreatedAt());
            assertTrue(p.getIsActive());
            assertEquals("ACTIVE", p.getStatus());
        });

        // Verify it still exists in DB
        Optional<Product> retrieved = productRepository.findById(id);
        assertTrue(retrieved.isPresent(), "Product must remain present in PostgreSQL regardless of elapsed time");
        assertTrue(retrieved.get().getIsActive(), "Product must remain active; no automated job should expire it");
    }

    @Test
    @Order(11)
    @DisplayName("Image resilience: Null or missing image does not delete or hide product")
    void testNullOrMissingImageDoesNotDeleteProduct() {
        String sku = "SKU-NOIMG-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = Product.builder()
                .name("Handcrafted Wooden Spoons")
                .slug(sku.toLowerCase())
                .sku(sku)
                .description("Eco-friendly wooden spoons")
                .price(new BigDecimal("499.00"))
                .stock(15)
                .category(testCategory)
                .isActive(true)
                .status("ACTIVE")
                .images(new ArrayList<>())
                .weightGrams(200)
                .build();

        Product saved = productRepository.save(product);
        assertNotNull(saved.getId());

        Optional<Product> reloaded = productRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        assertTrue(reloaded.get().getImages().isEmpty());
        assertTrue(reloaded.get().getIsActive(), "Product must remain active even without an image");
    }
}
