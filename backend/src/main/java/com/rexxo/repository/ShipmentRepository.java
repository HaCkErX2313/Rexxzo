package com.rexxo.repository;

import com.rexxo.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByProviderOrderId(String providerOrderId);

    Optional<Shipment> findByAwbNumber(String awbNumber);

    @Query("SELECT s FROM Shipment s WHERE s.order.id = :orderId")
    Optional<Shipment> findByOrderIdWithEvents(Long orderId);
}
