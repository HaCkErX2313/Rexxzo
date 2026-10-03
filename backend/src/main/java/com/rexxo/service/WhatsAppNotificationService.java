package com.rexxo.service;

import com.rexxo.entity.Notification;
import com.rexxo.entity.Order;
import com.rexxo.entity.ReturnRequest;
import com.rexxo.entity.Shipment;
import com.rexxo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.provider:meta}")
    private String provider;

    @Value("${whatsapp.api-url:}")
    private String apiUrl;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    public boolean isConfigured() {
        return apiUrl != null && !apiUrl.isBlank()
            && accessToken != null && !accessToken.isBlank()
            && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    public void sendOrderConfirmation(Order order) {
        String phone = order.getShippingPhone();
        String message = String.format("Hi %s, your REXXZO order #%s of amount ₹%s is confirmed! We will notify you once shipped.",
            order.getShippingName() != null ? order.getShippingName() : "Valued Customer",
            order.getOrderNumber(),
            order.getTotal());

        dispatch(order.getUser() != null ? order.getUser().getId() : null,
            order.getId(),
            phone,
            Notification.Type.ORDER_CONFIRMATION,
            message);
    }

    public void sendShipmentUpdate(Shipment shipment) {
        Order order = shipment.getOrder();
        String phone = order != null ? order.getShippingPhone() : null;
        String awb = shipment.getAwbNumber() != null ? shipment.getAwbNumber() : "N/A";
        String courier = shipment.getCourierName() != null ? shipment.getCourierName() : "Courier Partner";
        String message = String.format("Your REXXZO order #%s has been dispatched via %s. AWB/Tracking: %s",
            order != null ? order.getOrderNumber() : "Order", courier, awb);

        dispatch(order != null && order.getUser() != null ? order.getUser().getId() : null,
            order != null ? order.getId() : null,
            phone,
            Notification.Type.SHIPMENT_UPDATE,
            message);
    }

    public void sendOutForDelivery(Shipment shipment) {
        Order order = shipment.getOrder();
        String phone = order != null ? order.getShippingPhone() : null;
        String message = String.format("Your REXXZO package for order #%s is OUT FOR DELIVERY today! Please keep your phone reachable.",
            order != null ? order.getOrderNumber() : "Order");

        dispatch(order != null && order.getUser() != null ? order.getUser().getId() : null,
            order != null ? order.getId() : null,
            phone,
            Notification.Type.OUT_FOR_DELIVERY,
            message);
    }

    public void sendDeliveryConfirmation(Shipment shipment) {
        Order order = shipment.getOrder();
        String phone = order != null ? order.getShippingPhone() : null;
        String message = String.format("Your REXXZO order #%s has been DELIVERED. Thank you for choosing REXXZO! Enjoy your curated essentials.",
            order != null ? order.getOrderNumber() : "Order");

        dispatch(order != null && order.getUser() != null ? order.getUser().getId() : null,
            order != null ? order.getId() : null,
            phone,
            Notification.Type.DELIVERED,
            message);
    }

    public void sendReturnUpdate(ReturnRequest returnRequest) {
        Order order = returnRequest.getOrder();
        String phone = order != null ? order.getShippingPhone() : null;
        String message = String.format("Your REXXZO return request #%s for order #%s is now: %s.",
            returnRequest.getReturnNumber(),
            order != null ? order.getOrderNumber() : "Order",
            returnRequest.getStatus().name());

        dispatch(returnRequest.getUser() != null ? returnRequest.getUser().getId() : null,
            order != null ? order.getId() : null,
            phone,
            Notification.Type.RETURN_APPROVED,
            message);
    }

    private void dispatch(Long userId, Long orderId, String recipient, Notification.Type type, String text) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send WhatsApp notification: Recipient phone number is missing.");
            return;
        }

        // Avoid duplicate notification for the same event
        if (orderId != null && notificationRepository.existsByOrderIdAndTypeAndChannel(orderId, type, Notification.Channel.WHATSAPP)) {
            log.info("WhatsApp notification of type {} already recorded for order ID {}", type, orderId);
            return;
        }

        Notification record = Notification.builder()
            .userId(userId)
            .orderId(orderId)
            .channel(Notification.Channel.WHATSAPP)
            .type(type)
            .recipient(recipient)
            .status(Notification.Status.PENDING)
            .build();

        if (!isConfigured()) {
            record.setStatus(Notification.Status.DISABLED);
            record.setErrorMessage("WhatsApp provider credentials not configured (WHATSAPP_API_URL / WHATSAPP_ACCESS_TOKEN / WHATSAPP_PHONE_NUMBER_ID)");
            notificationRepository.save(record);
            log.info("WhatsApp notification recorded as DISABLED (configuration required): orderId={}, type={}", orderId, type);
            return;
        }

        // Real API invocation when credentials are provided
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            // Clean recipient phone (Indian numbers: standard E.164 without plus)
            String cleanPhone = recipient.replaceAll("[^0-9]", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("recipient_type", "individual");
            body.put("to", cleanPhone);
            body.put("type", "text");
            body.put("text", Map.of("preview_url", false, "body", text));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String targetUrl = apiUrl.contains("{phone_number_id}")
                ? apiUrl.replace("{phone_number_id}", phoneNumberId)
                : (apiUrl.endsWith("/") ? apiUrl + phoneNumberId + "/messages" : apiUrl + "/" + phoneNumberId + "/messages");

            ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                record.setStatus(Notification.Status.SENT);
                record.setSentAt(LocalDateTime.now());
                if (response.getBody() != null && response.getBody().containsKey("messages")) {
                    record.setProviderMessageId(String.valueOf(response.getBody().get("messages")));
                }
            } else {
                record.setStatus(Notification.Status.FAILED);
                record.setErrorMessage("Provider returned HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("WhatsApp API dispatch failed: {}", e.getMessage());
            record.setStatus(Notification.Status.FAILED);
            record.setErrorMessage(e.getMessage());
        }

        notificationRepository.save(record);
    }
}
