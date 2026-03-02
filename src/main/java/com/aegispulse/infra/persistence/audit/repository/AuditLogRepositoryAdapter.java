package com.aegispulse.infra.persistence.audit.repository;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.audit.repository.AuditLogRepository;
import com.aegispulse.infra.persistence.audit.entity.AuditLogJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * 감사로그 저장소 포트를 JPA 구현체로 연결한다.
 */
@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository auditLogJpaRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return auditLogJpaRepository.save(AuditLogJpaEntity.fromDomain(auditLog)).toDomain();
    }

    @Override
    public List<AuditLog> findRecent(String actorId, AuditAction action, AuditTargetType targetType, String targetId, int limit) {
        return auditLogJpaRepository.findByFilters(actorId, action, targetType, targetId, PageRequest.of(0, limit)).stream()
            .map(AuditLogJpaEntity::toDomain)
            .toList();
    }
}
