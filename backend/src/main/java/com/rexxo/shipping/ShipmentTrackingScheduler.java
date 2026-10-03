package com.rexxo.shipping;

import com.rexxo.entity.Shipment;
import com.rexxo.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically syncs tracking status from Shiprocket for active shipments.
 * <p>
 * Only syncs shipments that:
 * - Have an AWB number assigned
 * - Are not in a terminal status (DELIVERED, CANCELLED, RETURNED, RTO)
 * <p>
 * Runs every 30 minutes — does NOT aggressively poll Shiprocket.
 * When Shiprocket webhooks are configured, this serves as a fallback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentTrackingScheduler {

    private final ShipmentRepository shipmentRepository;
    private final ShiprocketService shiprocketService;

    /**
     * Runs every 30 minutes. Syncs tracking for all active, non-terminal shipments.
     * fixedDelay ensures previous sync finishes before next starts.
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000L)
    public void syncActiveShipments() {
        List<Shipment> activeShipments = shipmentRepository.findAll().stream()
            .filter(s -> s.getAwbNumber() != null && !s.getAwbNumber().isBlank())
            .filter(s -> !ShiprocketStatusMapper.isTerminal(s.getShipmentStatus()))
            .toList();

        if (activeShipments.isEmpty()) {
            return;
        }

        log.info("Scheduled tracking sync: {} active shipment(s)", activeShipments.size());

        for (Shipment shipment : activeShipments) {
            try {
                shiprocketService.syncTrackingFromProvider(shipment);
            } catch (Exception e) {
                log.warn("Tracking sync failed for shipment {}: {}", shipment.getId(), e.getMessage());
                // Continue with next shipment — don't let one failure stop others
            }
        }

        log.info("Scheduled tracking sync complete");
    }
}
