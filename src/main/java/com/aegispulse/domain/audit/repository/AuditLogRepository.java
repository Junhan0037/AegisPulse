package com.aegispulse.domain.audit.repository;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import java.util.List;

/**
 * 감사로그 저장소 추상화.
 */
public interface AuditLogRepository {

    /**
     * 감사로그를 저장한다.
     */
    AuditLog save(AuditLog auditLog);

    /**
     * 필터 조건으로 최신 감사로그를 조회한다.
     */
    List<AuditLog> findRecent(String actorId, AuditAction action, AuditTargetType targetType, String targetId, int limit);
}
