package com.rexxo.controller;

import com.rexxo.entity.AnalyticsEvent;
import com.rexxo.entity.User;
import com.rexxo.repository.UserRepository;
import com.rexxo.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> recordEvent(
            Authentication auth,
            @RequestBody EventRequest request) {

        Long userId = null;
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            userId = userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
        }

        AnalyticsEvent.EventType eventType;
        try {
            eventType = AnalyticsEvent.EventType.valueOf(request.eventType().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid event type"));
        }

        analyticsService.trackEvent(
            eventType,
            userId,
            request.sessionId(),
            request.entityType(),
            request.entityId(),
            request.metadata()
        );

        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    public record EventRequest(
        String eventType,
        String sessionId,
        String entityType,
        Long entityId,
        String metadata
    ) {}
}
