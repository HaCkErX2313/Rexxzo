package com.rexxo.repository;

import com.rexxo.entity.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {

    List<ShipmentTrackingEvent> findByShipmentIdOrderByEventTimestampAsc(Long shipmentId);

    Optional<ShipmentTrackingEvent> findByShipmentIdAndProviderEventId(Long shipmentId, String providerEventId);

    boolean existsByShipmentIdAndProviderEventId(Long shipmentId, String providerEventId);
}
