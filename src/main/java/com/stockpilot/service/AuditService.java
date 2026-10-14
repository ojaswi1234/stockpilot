package com.stockpilot.service;

import com.stockpilot.model.AuditLog;
import com.stockpilot.model.User;
import com.stockpilot.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User user, String action, String resourceType, String resourceId, String details) {
        String username = user != null ? user.getUsername() : "SYSTEM";
        AuditLog log = new AuditLog(user, username, action, resourceType, resourceId, details);
        auditLogRepository.save(log);
    }
}
