package com.rexxo.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.controller.CartController.AddItemRequest;
import com.rexxo.entity.Cart;
import com.rexxo.entity.Category;
import com.rexxo.entity.Product;
import com.rexxo.entity.User;
import com.rexxo.repository.CartItemRepository;
import com.rexxo.repository.CartRepository;
import com.rexxo.repository.CategoryRepository;
import com.rexxo.repository.ProductRepository;
import com.rexxo.repository.UserRepository;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private static final String TEST_USER_EMAIL = "carttestuser@rexxo.com";
    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail(TEST_USER_EMAIL).orElseGet(() -> {
            User u = User.builder()
                .name("Cart Test User")
                .email(TEST_USER_EMAIL)
                .phone("+919988776655")
                .password("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
            return userRepository.save(u);
        });

        testCart = cartRepository.findByUserId(testUser.getId()).orElseGet(() -> {
            Cart c = Cart.builder().user(testUser).build();
            return cartRepository.save(c);
        });

        // Clean out items before test
        cartItemRepository.deleteByCartId(testCart.getId());

        Category testCat = categoryRepository.findBySlug("cart-test-cat").orElseGet(() -> {
            Category cat = Category.builder()
                .name("Cart Test Cat")
                .slug("cart-test-cat")
                .build();
            return categoryRepository.save(cat);
        });

        testProduct = productRepository.findBySlug("cart-test-product").orElseGet(() -> {
            Product p = Product.builder()
                .name("Cart Test Product")
                .slug("cart-test-product")
                .sku("CART-TEST-01")
                .price(new BigDecimal("1500.00"))
                .stock(5) // max stock = 5
                .category(testCat)
                .isActive(true)
                .build();
            return productRepository.save(p);
        });
        testProduct.setStock(5);
        testProduct.setPrice(new BigDecimal("1500.00"));
        testProduct.setCategory(testCat);
        testProduct.setIsActive(true);
        productRepository.save(testProduct);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @WithMockUser(username = TEST_USER_EMAIL)
    @DisplayName("Should add product to cart and compute subtotal using DB price")
    void testAddProductToCart() throws Exception {
        AddItemRequest req = new AddItemRequest(testProduct.getId(), 1);

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].quantity").value(1))
            .andExpect(jsonPath("$.items[0].price").value(1500.00))
            .andExpect(jsonPath("$.items[0].subtotal").value(1500.00))
            .andExpect(jsonPath("$.items[0].stockQuantity").value(5))
            .andExpect(jsonPath("$.subtotal").value(1500.00));
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @WithMockUser(username = TEST_USER_EMAIL)
    @DisplayName("Should increase quantity (+1) and update totals and units")
    void testIncreaseQuantity() throws Exception {
        // Add 1
        AddItemRequest addReq = new AddItemRequest(testProduct.getId(), 1);
        MvcResult addRes = mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isOk())
            .andReturn();

        Long itemId = objectMapper.readTree(addRes.getResponse().getContentAsString())
            .get("items").get(0).get("id").asLong();

        // Increase to 2
        mockMvc.perform(put("/api/cart/items/" + itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].subtotal").value(3000.00))
            .andExpect(jsonPath("$.subtotal").value(3000.00))
            .andExpect(jsonPath("$.itemCount").value(2));

        // Increase to 3
        mockMvc.perform(put("/api/cart/items/" + itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", 3))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(3))
            .andExpect(jsonPath("$.items[0].subtotal").value(4500.00))
            .andExpect(jsonPath("$.subtotal").value(4500.00))
            .andExpect(jsonPath("$.itemCount").value(3));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @WithMockUser(username = TEST_USER_EMAIL)
    @DisplayName("Should decrease quantity (-1) from 3 to 2, and 2 to 1")
    void testDecreaseQuantity() throws Exception {
        // Add initial 3
        AddItemRequest addReq = new AddItemRequest(testProduct.getId(), 3);
        MvcResult addRes = mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isOk())
            .andReturn();

        Long itemId = objectMapper.readTree(addRes.getResponse().getContentAsString())
            .get("items").get(0).get("id").asLong();

        // Decrease 3 -> 2
        mockMvc.perform(put("/api/cart/items/" + itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].subtotal").value(3000.00))
            .andExpect(jsonPath("$.subtotal").value(3000.00));

        // Decrease 2 -> 1
        mockMvc.perform(put("/api/cart/items/" + itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(1))
            .andExpect(jsonPath("$.items[0].subtotal").value(1500.00))
            .andExpect(jsonPath("$.subtotal").value(1500.00));
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @WithMockUser(username = TEST_USER_EMAIL)
    @DisplayName("Should reject quantity exceeding available stock (stock is 5, requested is 6)")
    void testRejectExceedingStock() throws Exception {
        AddItemRequest addReq = new AddItemRequest(testProduct.getId(), 1);
        MvcResult addRes = mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isOk())
            .andReturn();

        Long itemId = objectMapper.readTree(addRes.getResponse().getContentAsString())
            .get("items").get(0).get("id").asLong();

        // Try setting quantity = 6 (stock is 5)
        mockMvc.perform(put("/api/cart/items/" + itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", 6))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Insufficient stock. Only 5 available."));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @WithMockUser(username = TEST_USER_EMAIL)
    @DisplayName("Should remove item from cart and recalculate subtotal and itemCount to zero")
    void testRemoveCartItem() throws Exception {
        AddItemRequest addReq = new AddItemRequest(testProduct.getId(), 2);
        MvcResult addRes = mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isOk())
            .andReturn();

        Long itemId = objectMapper.readTree(addRes.getResponse().getContentAsString())
            .get("items").get(0).get("id").asLong();

        // Remove item
        mockMvc.perform(delete("/api/cart/items/" + itemId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.subtotal").value(0))
            .andExpect(jsonPath("$.itemCount").value(0));
    }
}
