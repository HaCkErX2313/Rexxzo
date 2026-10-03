package com.rexxo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_tracking_events", indexes = {
    @Index(name = "idx_tracking_shipment", columnList = "shipment_id"),
    @Index(name = "idx_tracking_event_time", columnList = "event_timestamp"),
    @Index(name = "idx_tracking_provider_event", columnList = "shipment_id,provider_event_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    /**
     * Identifier from the provider to prevent duplicate event storage.
     * May be composed of status + location + timestamp if no unique ID provided.
     */
    @Column(name = "provider_event_id", length = 200)
    private String providerEventId;

    /** Mapped internal REXXO status */
    @Column(name = "status", length = 50)
    private String status;

    /** Raw status string from Shiprocket */
    @Column(name = "raw_status", length = 200)
    private String rawStatus;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
