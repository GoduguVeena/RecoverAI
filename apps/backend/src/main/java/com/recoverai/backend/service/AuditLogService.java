package com.recoverai.backend.service;

import com.recoverai.backend.domain.entity.AuditLog;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logEvent(String entityType, UUID entityId, String eventType, ActorType actorType, String actorId, String action, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setEventType(eventType);
        auditLog.setActorType(actorType);
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setReason(reason);
        auditLogRepository.save(auditLog);
    }
}
