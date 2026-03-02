package com.aegispulse.application.audit;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;

/**
 * 감사로그 기록 유스케이스 계약.
 */
public interface AuditLogWriteUseCase {

    void record(
        AuditAction action,
        AuditTargetType targetType,
        String targetId,
        String actorId,
        String traceId,
        String beforeJson,
        String afterJson
    );
}
