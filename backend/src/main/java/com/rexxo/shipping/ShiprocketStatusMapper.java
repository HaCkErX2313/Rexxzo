package com.rexxo.shipping;

import com.rexxo.entity.Order;
import com.rexxo.entity.Shipment;

/**
 * Maps raw Shiprocket status strings to internal REXXO statuses.
 * <p>
 * Shiprocket statuses reference:
 * https://apidocs.shiprocket.in/#tracking
 */
public final class ShiprocketStatusMapper {

    private ShiprocketStatusMapper() {}

    /**
     * Maps a raw Shiprocket shipment status to the internal Shipment.ShipmentStatus.
     */
    public static Shipment.ShipmentStatus toShipmentStatus(String rawStatus) {
        if (rawStatus == null) return Shipment.ShipmentStatus.CREATION_PENDING;

        String s = rawStatus.toUpperCase().strip();
        return switch (s) {
            case "NEW", "PENDING", "READY TO SHIP", "PICKUP PENDING" -> Shipment.ShipmentStatus.AWB_ASSIGNED;
            case "PICKUP SCHEDULED", "PICKUP QUEUED" -> Shipment.ShipmentStatus.PICKUP_SCHEDULED;
            case "PICKED UP", "PICK UP DONE" -> Shipment.ShipmentStatus.PICKED_UP;
            case "IN TRANSIT", "REACHED AT DESTINATION HUB",
                 "REACHED AT SOURCE HUB", "DISPATCHED" -> Shipment.ShipmentStatus.IN_TRANSIT;
            case "OUT FOR DELIVERY" -> Shipment.ShipmentStatus.OUT_FOR_DELIVERY;
            case "DELIVERED" -> Shipment.ShipmentStatus.DELIVERED;
            case "CANCELLED", "SHIPMENT CANCELLED", "PICKUP CANCELLED" -> Shipment.ShipmentStatus.CANCELLED;
            case "RETURN INITIATED", "RETURN ORDER MANIFESTED" -> Shipment.ShipmentStatus.RETURN_INITIATED;
            case "RETURN IN TRANSIT" -> Shipment.ShipmentStatus.RETURN_IN_TRANSIT;
            case "RTO INITIATED", "RTO DELIVERED", "RTO IN TRANSIT" -> Shipment.ShipmentStatus.RTO;
            case "RETURNED" -> Shipment.ShipmentStatus.RETURNED;
            default -> Shipment.ShipmentStatus.IN_TRANSIT;
        };
    }

    /**
     * Maps a Shipment.ShipmentStatus to the corresponding Order.OrderStatus.
     * Order status is updated when shipment status changes.
     */
    public static Order.OrderStatus toOrderStatus(Shipment.ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus) {
            case CREATION_PENDING, CREATED -> Order.OrderStatus.CONFIRMED;
            case AWB_ASSIGNED -> Order.OrderStatus.PROCESSING;
            case PICKUP_SCHEDULED -> Order.OrderStatus.PICKUP_SCHEDULED;
            case PICKED_UP -> Order.OrderStatus.PICKED_UP;
            case IN_TRANSIT, RETURN_INITIATED, RETURN_IN_TRANSIT -> Order.OrderStatus.IN_TRANSIT;
            case OUT_FOR_DELIVERY -> Order.OrderStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> Order.OrderStatus.DELIVERED;
            case CANCELLED -> Order.OrderStatus.CANCELLED;
            case RTO -> Order.OrderStatus.RTO;
            case RETURNED -> Order.OrderStatus.RETURNED;
            case FAILED -> Order.OrderStatus.CONFIRMED;
        };
    }

    /**
     * Returns a human-readable status string for the frontend.
     */
    public static String toDisplayStatus(Shipment.ShipmentStatus status) {
        return switch (status) {
            case CREATION_PENDING -> "Order Placed";
            case CREATED -> "Order Confirmed";
            case AWB_ASSIGNED -> "Processing";
            case PICKUP_SCHEDULED -> "Pickup Scheduled";
            case PICKED_UP -> "Picked Up";
            case IN_TRANSIT -> "In Transit";
            case OUT_FOR_DELIVERY -> "Out for Delivery";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            case RETURN_INITIATED -> "Return Initiated";
            case RETURN_IN_TRANSIT -> "Return In Transit";
            case RTO -> "Return to Origin";
            case RETURNED -> "Returned";
            case FAILED -> "Shipment Failed";
        };
    }

    /** Returns true if the status is terminal (no further updates expected). */
    public static boolean isTerminal(Shipment.ShipmentStatus status) {
        return switch (status) {
            case DELIVERED, CANCELLED, RETURNED, RTO -> true;
            default -> false;
        };
    }
}
