package com.rexxo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rexxo.entity.AnalyticsEvent;
import com.rexxo.entity.Order;
import com.rexxo.entity.Product;
import com.rexxo.entity.Refund;
import com.rexxo.entity.ReturnRequest;
import com.rexxo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsEventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RefundRepository refundRepository;
    private final ReturnRequestRepository returnRequestRepository;

    @Async
    public void trackEvent(AnalyticsEvent.EventType eventType, Long userId, String sessionId,
                           String entityType, Long entityId, String metadata) {
        try {
            AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .sessionId(sessionId)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();
            eventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record analytics event: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getAnalytics(String range) {
        LocalDateTime since = resolveSince(range);

        List<Order> allOrders = orderRepository.findAll().stream()
            .filter(o -> since == null || (o.getCreatedAt() != null && o.getCreatedAt().isAfter(since)))
            .toList();

        long totalOrders = allOrders.size();

        // Revenue from paid or delivered orders
        BigDecimal totalRevenue = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED
                      || o.getStatus() == Order.OrderStatus.PAID
                      || o.getStatus() == Order.OrderStatus.CONFIRMED
                      || o.getStatus() == Order.OrderStatus.PROCESSING
                      || o.getStatus() == Order.OrderStatus.PACKED
                      || o.getStatus() == Order.OrderStatus.SHIPPED
                      || o.getStatus() == Order.OrderStatus.IN_TRANSIT
                      || o.getStatus() == Order.OrderStatus.OUT_FOR_DELIVERY)
            .map(Order::getTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrders > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long pendingOrders = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.PENDING
                      || o.getStatus() == Order.OrderStatus.PENDING_PAYMENT)
            .count();

        long deliveredOrders = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
            .count();

        long cancelledOrders = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED)
            .count();

        double cancellationRate = totalOrders > 0
            ? (double) cancelledOrders / totalOrders * 100
            : 0.0;

        List<ReturnRequest> allReturns = returnRequestRepository.findAll().stream()
            .filter(r -> since == null || (r.getCreatedAt() != null && r.getCreatedAt().isAfter(since)))
            .toList();
        long returnRequestsCount = allReturns.size();

        double returnRate = deliveredOrders > 0
            ? (double) returnRequestsCount / deliveredOrders * 100
            : 0.0;

        List<Refund> allRefunds = refundRepository.findAll().stream()
            .filter(r -> since == null || (r.getCreatedAt() != null && r.getCreatedAt().isAfter(since)))
            .toList();
        BigDecimal totalRefundAmount = allRefunds.stream()
            .map(Refund::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Low stock & out of stock products
        List<Product> allProducts = productRepository.findAll();
        long lowStockCount = allProducts.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getStock() > 0 && p.getStock() < 10)
            .count();
        long outOfStockCount = allProducts.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getStock() <= 0)
            .count();

        // Valid order statuses that represent genuine completed / in-progress paid sales
        Set<Order.OrderStatus> validRevenueStatuses = Set.of(
            Order.OrderStatus.DELIVERED,
            Order.OrderStatus.PAID,
            Order.OrderStatus.CONFIRMED,
            Order.OrderStatus.PROCESSING,
            Order.OrderStatus.PACKED,
            Order.OrderStatus.SHIPPED,
            Order.OrderStatus.IN_TRANSIT,
            Order.OrderStatus.OUT_FOR_DELIVERY
        );

        List<Order> validOrders = allOrders.stream()
            .filter(o -> o.getStatus() != null && validRevenueStatuses.contains(o.getStatus()))
            .toList();

        // Top selling products by order quantity
        Map<Long, Integer> productQtyMap = new HashMap<>();
        Map<Long, String> productNameMap = new HashMap<>();
        Map<Long, BigDecimal> productRevenueMap = new HashMap<>();

        for (Order o : validOrders) {
            if (o.getItems() != null) {
                for (var item : o.getItems()) {
                    Long pid = item.getProduct() != null ? item.getProduct().getId() : null;
                    if (pid != null) {
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        productQtyMap.merge(pid, qty, Integer::sum);

                        String name = item.getProductName();
                        if (name == null || name.isBlank()) {
                            name = item.getProduct() != null ? item.getProduct().getName() : "Product #" + pid;
                        }
                        productNameMap.put(pid, name);

                        BigDecimal itemSubtotal = item.getSubtotal();
                        if (itemSubtotal == null) {
                            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                            itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                        }
                        productRevenueMap.merge(pid, itemSubtotal, BigDecimal::add);
                    }
                }
            }
        }

        List<TopProductDto> topProducts = productQtyMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> new TopProductDto(
                e.getKey(),
                productNameMap.getOrDefault(e.getKey(), "Product #" + e.getKey()),
                e.getValue() != null ? e.getValue() : 0,
                productRevenueMap.getOrDefault(e.getKey(), BigDecimal.ZERO)
            ))
            .toList();

        // Top Categories
        Map<String, Integer> categorySalesMap = new HashMap<>();
        Map<String, BigDecimal> categoryRevenueMap = new HashMap<>();
        for (Order o : validOrders) {
            if (o.getItems() != null) {
                for (var item : o.getItems()) {
                    String catName = null;
                    if (item.getProduct() != null && item.getProduct().getCategory() != null) {
                        catName = item.getProduct().getCategory().getName();
                    }
                    if (catName != null && !catName.isBlank()) {
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        BigDecimal itemSubtotal = item.getSubtotal();
                        if (itemSubtotal == null) {
                            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                            itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                        }
                        categorySalesMap.merge(catName, qty, Integer::sum);
                        categoryRevenueMap.merge(catName, itemSubtotal, BigDecimal::add);
                    }
                }
            }
        }
        List<TopCategoryDto> topCategories = categorySalesMap.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> new TopCategoryDto(
                e.getKey(),
                e.getValue() != null ? e.getValue() : 0,
                categoryRevenueMap.getOrDefault(e.getKey(), BigDecimal.ZERO)
            ))
            .toList();

        return new AnalyticsSummaryDto(
            range != null ? range : "all",
            totalRevenue,
            totalOrders,
            averageOrderValue,
            pendingOrders,
            deliveredOrders,
            cancelledOrders,
            BigDecimal.valueOf(cancellationRate).setScale(1, RoundingMode.HALF_UP),
            returnRequestsCount,
            BigDecimal.valueOf(returnRate).setScale(1, RoundingMode.HALF_UP),
            totalRefundAmount,
            lowStockCount,
            outOfStockCount,
            topProducts,
            topCategories
        );
    }

    private LocalDateTime resolveSince(String range) {
        if (range == null || range.equalsIgnoreCase("all")) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (range.toLowerCase()) {
            case "today" -> LocalDate.now().atStartOfDay();
            case "7d", "7days" -> now.minusDays(7);
            case "30d", "30days" -> now.minusDays(30);
            case "90d", "90days" -> now.minusDays(90);
            default -> now.minusDays(30);
        };
    }

    public record AnalyticsSummaryDto(
        String range,
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal averageOrderValue,
        long pendingOrders,
        long deliveredOrders,
        long cancelledOrders,
        BigDecimal cancellationRate,
        long returnRequestsCount,
        BigDecimal returnRate,
        BigDecimal totalRefundAmount,
        long lowStockCount,
        long outOfStockCount,
        List<TopProductDto> topProducts,
        List<TopCategoryDto> topCategories
    ) {}

    public record TopProductDto(
        Long productId,
        String productName,
        int unitsSold,
        BigDecimal revenue
    ) {
        @JsonProperty("name")
        public String name() {
            return productName;
        }

        @JsonProperty("totalRevenue")
        public BigDecimal totalRevenue() {
            return revenue;
        }
    }

    public record TopCategoryDto(
        String categoryName,
        int unitsSold,
        BigDecimal revenue
    ) {}
}
