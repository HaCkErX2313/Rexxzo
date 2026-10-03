package com.rexxo.service;

import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final NotificationService notificationService;
    private final AdminAuditService auditService;

    private static final int RETURN_WINDOW_DAYS = 7;

    public ReturnRequest requestReturn(User user, CreateReturnRequestDto request) {
        Order order = orderRepository.findById(request.orderId())
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        // 1. Verify order ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("You do not have permission to return items for this order");
        }

        // 2. Verify delivered status
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw RexxoException.badRequest("Returns can only be requested for orders that have been successfully delivered");
        }

        // 3. Verify return window (7 days from updatedAt/delivered time)
        LocalDateTime deliveredTime = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        long daysSinceDelivery = ChronoUnit.DAYS.between(deliveredTime, LocalDateTime.now());
        if (daysSinceDelivery > RETURN_WINDOW_DAYS) {
            throw RexxoException.badRequest("The return window of " + RETURN_WINDOW_DAYS + " days has expired for this order");
        }

        // 4. Prevent duplicate return requests
        List<ReturnRequest.ReturnStatus> activeStatuses = List.of(
            ReturnRequest.ReturnStatus.RETURN_REQUESTED,
            ReturnRequest.ReturnStatus.RETURN_APPROVED,
            ReturnRequest.ReturnStatus.PICKUP_SCHEDULED,
            ReturnRequest.ReturnStatus.PICKED_UP,
            ReturnRequest.ReturnStatus.RECEIVED,
            ReturnRequest.ReturnStatus.QUALITY_CHECK,
            ReturnRequest.ReturnStatus.REFUND_PENDING,
            ReturnRequest.ReturnStatus.REFUNDED
        );
        if (returnRequestRepository.existsByOrderIdAndStatusNotIn(order.getId(),
            List.of(ReturnRequest.ReturnStatus.RETURN_REJECTED, ReturnRequest.ReturnStatus.RETURN_CANCELLED))) {
            throw RexxoException.conflict("An active return request already exists for this order");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw RexxoException.badRequest("At least one item must be selected for return");
        }

        // 5. Validate items and build return request
        String returnNumber = "RET-" + System.currentTimeMillis();
        ReturnRequest returnRequest = ReturnRequest.builder()
            .returnNumber(returnNumber)
            .order(order)
            .user(user)
            .status(ReturnRequest.ReturnStatus.RETURN_REQUESTED)
            .reason(request.reason())
            .description(request.description())
            .build();

        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnItemDto itemDto : request.items()) {
            OrderItem orderItem = order.getItems().stream()
                .filter(i -> i.getId().equals(itemDto.orderItemId()))
                .findFirst()
                .orElseThrow(() -> RexxoException.badRequest("Selected item is not part of this order"));

            if (itemDto.quantity() <= 0) {
                throw RexxoException.badRequest("Return quantity must be at least 1");
            }

            if (itemDto.quantity() > orderItem.getQuantity()) {
                throw RexxoException.badRequest("Return quantity (" + itemDto.quantity() +
                    ") cannot exceed purchased quantity (" + orderItem.getQuantity() + ") for " + orderItem.getProductName());
            }

            BigDecimal itemRefund = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.quantity()));
            totalRefundAmount = totalRefundAmount.add(itemRefund);

            ReturnItem returnItem = ReturnItem.builder()
                .returnRequest(returnRequest)
                .orderItem(orderItem)
                .product(orderItem.getProduct())
                .quantity(itemDto.quantity())
                .refundAmount(itemRefund)
                .build();
            returnItems.add(returnItem);
        }

        returnRequest.setRefundAmount(totalRefundAmount);
        returnRequest.setItems(returnItems);

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // Update order status to reflect return request
        order.setStatus(Order.OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    public ReturnRequest cancelReturn(User user, Long returnId) {
        ReturnRequest ret = returnRequestRepository.findByIdAndUserId(returnId, user.getId())
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));

        if (ret.getStatus() != ReturnRequest.ReturnStatus.RETURN_REQUESTED) {
            throw RexxoException.badRequest("Return request cannot be cancelled once approved or processed");
        }

        ret.setStatus(ReturnRequest.ReturnStatus.RETURN_CANCELLED);
        ReturnRequest saved = returnRequestRepository.save(ret);

        Order order = ret.getOrder();
        order.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(order);

        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequest> getUserReturns(Long userId, Pageable pageable) {
        return returnRequestRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public ReturnRequest getUserReturn(Long returnId, Long userId) {
        return returnRequestRepository.findByIdAndUserId(returnId, userId)
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));
    }

    // Admin Operations
    @Transactional(readOnly = true)
    public Page<ReturnRequest> getAllReturns(ReturnRequest.ReturnStatus status, Pageable pageable) {
        if (status != null) {
            return returnRequestRepository.findByStatus(status, pageable);
        }
        return returnRequestRepository.findAll(pageable);
    }

    public ReturnRequest adminApprove(Long returnId, String adminEmail, String adminNotes) {
        ReturnRequest ret = returnRequestRepository.findByIdWithItems(returnId)
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));

        if (ret.getStatus() != ReturnRequest.ReturnStatus.RETURN_REQUESTED) {
            throw RexxoException.badRequest("Only pending return requests can be approved");
        }

        ret.setStatus(ReturnRequest.ReturnStatus.RETURN_APPROVED);
        ret.setAdminNotes(adminNotes);
        ReturnRequest saved = returnRequestRepository.save(ret);

        auditService.log(adminEmail, "ADMIN_APPROVED_RETURN", "ReturnRequest", ret.getId(),
            "Approved return #" + ret.getReturnNumber(), null);
        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    public ReturnRequest adminReject(Long returnId, String adminEmail, String rejectionReason) {
        ReturnRequest ret = returnRequestRepository.findByIdWithItems(returnId)
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));

        if (ret.getStatus() != ReturnRequest.ReturnStatus.RETURN_REQUESTED) {
            throw RexxoException.badRequest("Only pending return requests can be rejected");
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw RexxoException.badRequest("Rejection reason is required");
        }

        ret.setStatus(ReturnRequest.ReturnStatus.RETURN_REJECTED);
        ret.setRejectionReason(rejectionReason);
        ReturnRequest saved = returnRequestRepository.save(ret);

        Order order = ret.getOrder();
        order.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(order);

        auditService.log(adminEmail, "ADMIN_REJECTED_RETURN", "ReturnRequest", ret.getId(),
            "Rejected return #" + ret.getReturnNumber() + ": " + rejectionReason, null);
        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    public ReturnRequest adminSchedulePickup(Long returnId, String adminEmail, String courier, String awb) {
        ReturnRequest ret = returnRequestRepository.findByIdWithItems(returnId)
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));

        ret.setStatus(ReturnRequest.ReturnStatus.PICKUP_SCHEDULED);
        ret.setPickupCourier(courier);
        ret.setPickupAwb(awb);
        ReturnRequest saved = returnRequestRepository.save(ret);

        auditService.log(adminEmail, "ADMIN_SCHEDULED_RETURN_PICKUP", "ReturnRequest", ret.getId(),
            "Scheduled pickup with " + courier + ", AWB: " + awb, null);
        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    public ReturnRequest adminMarkReceived(Long returnId, String adminEmail) {
        ReturnRequest ret = returnRequestRepository.findByIdWithItems(returnId)
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));

        ret.setStatus(ReturnRequest.ReturnStatus.RECEIVED);
        ReturnRequest saved = returnRequestRepository.save(ret);

        Order order = ret.getOrder();
        order.setStatus(Order.OrderStatus.RETURNED);
        orderRepository.save(order);

        auditService.log(adminEmail, "ADMIN_MARKED_RETURN_RECEIVED", "ReturnRequest", ret.getId(),
            "Product received at warehouse", null);
        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    public ReturnRequest adminApproveRefund(Long returnId, String adminEmail, String refundReason) {
        ReturnRequest ret = returnRequestRepository.findByIdWithItems(returnId)
            .orElseThrow(() -> RexxoException.notFound("Return request not found"));

        // Create or get Refund record
        String refundNumber = "REF-" + System.currentTimeMillis();
        Refund refund = ret.getRefund();
        if (refund == null) {
            refund = Refund.builder()
                .refundId(refundNumber)
                .order(ret.getOrder())
                .payment(ret.getOrder().getPayment())
                .amount(ret.getRefundAmount() != null ? ret.getRefundAmount() : ret.getOrder().getTotal())
                .status(Refund.RefundStatus.REFUND_PENDING) // Pending Razorpay credentials / provider execution
                .reason(refundReason != null ? refundReason : "Return approved for order #" + ret.getOrder().getOrderNumber())
                .provider("RAZORPAY")
                .build();
            refund = refundRepository.save(refund);
            ret.setRefund(refund);
        }

        ret.setStatus(ReturnRequest.ReturnStatus.REFUND_PENDING);
        ReturnRequest saved = returnRequestRepository.save(ret);

        auditService.log(adminEmail, "ADMIN_APPROVED_REFUND", "ReturnRequest", ret.getId(),
            "Refund pending approval of ₹" + refund.getAmount() + " (Refund ID: " + refund.getRefundId() + ")", null);
        notificationService.notifyReturnUpdate(saved);
        return saved;
    }

    // DTO records
    public record CreateReturnRequestDto(
        Long orderId,
        String reason,
        String description,
        List<ReturnItemDto> items
    ) {}

    public record ReturnItemDto(
        Long orderItemId,
        Integer quantity
    ) {}
}
