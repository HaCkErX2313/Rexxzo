package com.rexxo.shipping;

/**
 * Abstraction layer for logistics/shipping providers.
 * <p>
 * REXXO backend depends on this interface — not on Shiprocket directly.
 * Another provider (e.g. Delhivery) can be added by implementing this interface.
 */
public interface LogisticsService {

    /**
     * Checks if a delivery route is serviceable and returns available couriers with rates.
     */
    ShippingDtos.ServiceabilityResponse checkServiceability(ShippingDtos.ServiceabilityRequest request);

    /**
     * Creates a shipment for the given REXXO order and returns provider identifiers.
     * Safe to call multiple times — implementations must guard against duplicate creation.
     */
    ShippingDtos.CreateShipmentResponse createShipment(ShippingDtos.CreateShipmentRequest request);

    /**
     * Fetches latest tracking information from the provider.
     * Returns null if no tracking is available yet.
     */
    ShippingDtos.TrackingResponse getTracking(Long shipmentId);

    /**
     * Cancels a shipment at the provider level.
     * Returns true if cancellation succeeded or was already cancelled.
     */
    boolean cancelShipment(Long shipmentId);

    /**
     * Fetches label PDF URL / download link for a shipment.
     */
    String generateLabel(Long shipmentId);

    /**
     * Validates that the provider is configured and reachable.
     * Used by health/status endpoints.
     */
    boolean isHealthy();
}
