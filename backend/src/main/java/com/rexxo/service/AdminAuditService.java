package com.rexxo.service;

import com.rexxo.entity.AdminAuditLog;
import com.rexxo.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String adminEmail, String action, String entityType, Long entityId, String details, String ipAddress) {
        try {
            AdminAuditLog entry = AdminAuditLog.builder()
                .adminEmail(adminEmail != null ? adminEmail : "system")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write admin audit log: {}", e.getMessage());
        }
    }
}
