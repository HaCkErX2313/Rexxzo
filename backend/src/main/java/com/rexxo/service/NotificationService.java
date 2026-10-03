package com.rexxo.service;

import com.rexxo.entity.Order;
import com.rexxo.entity.ReturnRequest;
import com.rexxo.entity.Shipment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final WhatsAppNotificationService whatsAppService;

    @Async
    public void notifyOrderConfirmed(Order order) {
        try {
            whatsAppService.sendOrderConfirmation(order);
        } catch (Exception e) {
            log.warn("Non-fatal: failed to send order confirmation notification for order #{}: {}",
                order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyShipmentDispatched(Shipment shipment) {
        try {
            whatsAppService.sendShipmentUpdate(shipment);
        } catch (Exception e) {
            log.warn("Non-fatal: failed to send shipment dispatch notification: {}", e.getMessage());
        }
    }

    @Async
    public void notifyOutForDelivery(Shipment shipment) {
        try {
            whatsAppService.sendOutForDelivery(shipment);
        } catch (Exception e) {
            log.warn("Non-fatal: failed to send out-for-delivery notification: {}", e.getMessage());
        }
    }

    @Async
    public void notifyDelivered(Shipment shipment) {
        try {
            whatsAppService.sendDeliveryConfirmation(shipment);
        } catch (Exception e) {
            log.warn("Non-fatal: failed to send delivered notification: {}", e.getMessage());
        }
    }

    @Async
    public void notifyReturnUpdate(ReturnRequest returnRequest) {
        try {
            whatsAppService.sendReturnUpdate(returnRequest);
        } catch (Exception e) {
            log.warn("Non-fatal: failed to send return notification: {}", e.getMessage());
        }
    }
}
