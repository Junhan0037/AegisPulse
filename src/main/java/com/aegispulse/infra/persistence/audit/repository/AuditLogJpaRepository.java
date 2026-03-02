package com.aegispulse.infra.persistence.audit.repository;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.infra.persistence.audit.entity.AuditLogJpaEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 감사로그 JPA 리포지토리.
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, String> {

    @Query(
        """
        SELECT a
        FROM AuditLogJpaEntity a
        WHERE (:actorId IS NULL OR a.actorId = :actorId)
          AND (:action IS NULL OR a.action = :action)
          AND (:targetType IS NULL OR a.targetType = :targetType)
          AND (:targetId IS NULL OR a.targetId = :targetId)
        ORDER BY a.timestamp DESC
        """
    )
    List<AuditLogJpaEntity> findByFilters(
        @Param("actorId") String actorId,
        @Param("action") AuditAction action,
        @Param("targetType") AuditTargetType targetType,
        @Param("targetId") String targetId,
        Pageable pageable
    );
}
