package com.rexxo.controller;

import com.rexxo.entity.Order;
import com.rexxo.entity.ReturnRequest;
import com.rexxo.entity.User;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.OrderRepository;
import com.rexxo.repository.UserRepository;
import com.rexxo.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<ReturnDto> createReturn(Authentication auth,
                                                  @RequestBody ReturnService.CreateReturnRequestDto request) {
        User user = getUser(auth);
        ReturnRequest ret = returnService.requestReturn(user, request);
        return ResponseEntity.ok(toDto(ret));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ReturnDto>> myReturns(Authentication auth,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        User user = getUser(auth);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(returnService.getUserReturns(user.getId(), pageable).map(this::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnDto> getReturn(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        return ResponseEntity.ok(toDto(returnService.getUserReturn(id, user.getId())));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReturnDto> cancelReturn(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        return ResponseEntity.ok(toDto(returnService.cancelReturn(user, id)));
    }

    @GetMapping("/orders/{orderId}/eligibility")
    public ResponseEntity<EligibilityDto> checkEligibility(Authentication auth, @PathVariable Long orderId) {
        User user = getUser(auth);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("Access denied");
        }

        boolean delivered = order.getStatus() == Order.OrderStatus.DELIVERED;
        LocalDateTime deliveredTime = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        long daysSinceDelivery = ChronoUnit.DAYS.between(deliveredTime, LocalDateTime.now());
        boolean withinWindow = daysSinceDelivery <= 7;

        boolean eligible = delivered && withinWindow;
        String reason = null;
        if (!delivered) {
            reason = "Only delivered orders are eligible for return.";
        } else if (!withinWindow) {
            reason = "The 7-day return window has expired for this order.";
        }

        List<EligibleItemDto> items = order.getItems().stream().map(i ->
            new EligibleItemDto(i.getId(), i.getProduct().getId(), i.getProductName(),
                i.getProductImageUrl(), i.getUnitPrice(), i.getQuantity())
        ).toList();

        return ResponseEntity.ok(new EligibilityDto(eligible, reason, 7 - (int) Math.min(daysSinceDelivery, 7), items));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    private ReturnDto toDto(ReturnRequest r) {
        List<ReturnItemDto> items = r.getItems().stream().map(i ->
            new ReturnItemDto(i.getId(), i.getOrderItem().getId(), i.getProduct().getId(),
                i.getOrderItem().getProductName(), i.getQuantity(), i.getRefundAmount())
        ).toList();

        return new ReturnDto(
            r.getId(),
            r.getReturnNumber(),
            r.getOrder().getId(),
            r.getOrder().getOrderNumber(),
            r.getStatus().name(),
            r.getReason(),
            r.getDescription(),
            r.getRejectionReason(),
            r.getPickupCourier(),
            r.getPickupAwb(),
            r.getRefundAmount(),
            r.getRefund() != null ? r.getRefund().getRefundId() : null,
            r.getRefund() != null ? r.getRefund().getStatus().name() : null,
            items,
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }

    public record ReturnDto(
        Long id,
        String returnNumber,
        Long orderId,
        String orderNumber,
        String status,
        String reason,
        String description,
        String rejectionReason,
        String pickupCourier,
        String pickupAwb,
        BigDecimal refundAmount,
        String refundId,
        String refundStatus,
        List<ReturnItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}

    public record ReturnItemDto(
        Long id,
        Long orderItemId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal refundAmount
    ) {}

    public record EligibilityDto(
        boolean eligible,
        String message,
        int daysRemaining,
        List<EligibleItemDto> eligibleItems
    ) {}

    public record EligibleItemDto(
        Long orderItemId,
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        Integer purchasedQuantity
    ) {}
}
