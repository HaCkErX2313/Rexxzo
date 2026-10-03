package com.rexxo.repository;

import com.rexxo.entity.AnalyticsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countByEventTypeAndCreatedAtAfter(AnalyticsEvent.EventType eventType, LocalDateTime after);

    long countByCreatedAtAfter(LocalDateTime after);

    Page<AnalyticsEvent> findByEventTypeOrderByCreatedAtDesc(AnalyticsEvent.EventType eventType, Pageable pageable);
}
