package com.rexxo.service;

import com.rexxo.entity.*;
import com.rexxo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsEventRepository eventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
            eventRepository,
            orderRepository,
            productRepository,
            refundRepository,
            returnRequestRepository
        );
    }

    @Test
    @DisplayName("Empty database returns zero values and empty topProducts without errors")
    void testEmptyDatabase() {
        when(orderRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAll()).thenReturn(List.of());
        when(refundRepository.findAll()).thenReturn(List.of());
        when(returnRequestRepository.findAll()).thenReturn(List.of());

        AnalyticsService.AnalyticsSummaryDto dto = analyticsService.getAnalytics("30d");

        assertThat(dto).isNotNull();
        assertThat(dto.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.totalOrders()).isEqualTo(0);
        assertThat(dto.topProducts()).isEmpty();
        assertThat(dto.topCategories()).isEmpty();
    }

    @Test
    @DisplayName("Delivered order calculates topProducts with correct productName, unitsSold, and revenue")
    void testDeliveredOrderTopProductsCalculation() {
        Category category = Category.builder()
            .id(1L)
            .name("Home Decor")
            .slug("home-decor")
            .build();

        Product product = Product.builder()
            .id(101L)
            .name("Artisan Ceramic Vase")
            .price(BigDecimal.valueOf(1499))
            .category(category)
            .stock(20)
            .build();

        OrderItem item = OrderItem.builder()
            .id(501L)
            .product(product)
            .productName("Artisan Ceramic Vase")
            .quantity(3)
            .unitPrice(BigDecimal.valueOf(1499))
            .subtotal(BigDecimal.valueOf(4497))
            .build();

        Order order = Order.builder()
            .id(1L)
            .orderNumber("ORD-1001")
            .status(Order.OrderStatus.DELIVERED)
            .total(BigDecimal.valueOf(4497))
            .subtotal(BigDecimal.valueOf(4497))
            .createdAt(LocalDateTime.now())
            .items(new ArrayList<>(List.of(item)))
            .build();
        item.setOrder(order);

        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(refundRepository.findAll()).thenReturn(List.of());
        when(returnRequestRepository.findAll()).thenReturn(List.of());

        AnalyticsService.AnalyticsSummaryDto dto = analyticsService.getAnalytics("30d");

        assertThat(dto.topProducts()).hasSize(1);
        AnalyticsService.TopProductDto topProduct = dto.topProducts().get(0);
        assertThat(topProduct.productId()).isEqualTo(101L);
        assertThat(topProduct.productName()).isEqualTo("Artisan Ceramic Vase");
        assertThat(topProduct.unitsSold()).isEqualTo(3);
        assertThat(topProduct.revenue()).isEqualByComparingTo(BigDecimal.valueOf(4497));
        // Compatibility Jackson mappings
        assertThat(topProduct.name()).isEqualTo("Artisan Ceramic Vase");
        assertThat(topProduct.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(4497));
    }

    @Test
    @DisplayName("Cancelled and refunded orders are excluded from revenue and top products")
    void testCancelledOrderExcluded() {
        Category category = Category.builder().id(1L).name("Lighting").slug("lighting").build();
        Product product = Product.builder().id(102L).name("Desk Lamp").price(BigDecimal.valueOf(800)).category(category).stock(10).build();

        OrderItem item = OrderItem.builder()
            .id(502L)
            .product(product)
            .productName("Desk Lamp")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(800))
            .subtotal(BigDecimal.valueOf(1600))
            .build();

        Order cancelledOrder = Order.builder()
            .id(2L)
            .orderNumber("ORD-1002")
            .status(Order.OrderStatus.CANCELLED)
            .total(BigDecimal.valueOf(1600))
            .subtotal(BigDecimal.valueOf(1600))
            .createdAt(LocalDateTime.now())
            .items(new ArrayList<>(List.of(item)))
            .build();

        when(orderRepository.findAll()).thenReturn(List.of(cancelledOrder));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(refundRepository.findAll()).thenReturn(List.of());
        when(returnRequestRepository.findAll()).thenReturn(List.of());

        AnalyticsService.AnalyticsSummaryDto dto = analyticsService.getAnalytics("30d");

        assertThat(dto.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.topProducts()).isEmpty();
    }
}
