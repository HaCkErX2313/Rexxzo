package com.rexxo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public health check endpoint.
 * GET /api/health — returns database connectivity status.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("status", "UP");

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
        } catch (Exception e) {
            status.put("database", "DOWN");
            status.put("status", "DEGRADED");
        }

        return ResponseEntity.ok(status);
    }
}
