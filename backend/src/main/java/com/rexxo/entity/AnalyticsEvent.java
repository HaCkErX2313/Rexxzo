package com.rexxo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_analytics_event_type", columnList = "event_type"),
    @Index(name = "idx_analytics_created_at", columnList = "created_at"),
    @Index(name = "idx_analytics_user", columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EventType {
        PRODUCT_VIEW,
        SEARCH,
        ADD_TO_CART,
        REMOVE_FROM_CART,
        CHECKOUT_STARTED,
        ORDER_CREATED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        REFUND_CREATED,
        RETURN_REQUESTED
    }
}
