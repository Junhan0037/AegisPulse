package com.aegispulse.infra.persistence.audit.entity;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 감사로그 JPA 엔티티.
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_target_type_target_id_ts", columnList = "target_type, target_id, timestamp"),
        @Index(name = "idx_audit_logs_actor_id_ts", columnList = "actor_id, timestamp"),
        @Index(name = "idx_audit_logs_action_ts", columnList = "action, timestamp")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(name = "actor_id", nullable = false, length = 120)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private AuditTargetType targetType;

    @Column(name = "target_id", nullable = false, length = 120)
    private String targetId;

    @Lob
    @Column(name = "before_json", nullable = false)
    private String beforeJson;

    @Lob
    @Column(name = "after_json", nullable = false)
    private String afterJson;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "trace_id", nullable = false, length = 80)
    private String traceId;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = "aud_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static AuditLogJpaEntity fromDomain(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(auditLog.getId());
        entity.setActorId(auditLog.getActorId());
        entity.setAction(auditLog.getAction());
        entity.setTargetType(auditLog.getTargetType());
        entity.setTargetId(auditLog.getTargetId());
        entity.setBeforeJson(auditLog.getBeforeJson());
        entity.setAfterJson(auditLog.getAfterJson());
        entity.setTimestamp(auditLog.getTimestamp());
        entity.setTraceId(auditLog.getTraceId());
        return entity;
    }

    public AuditLog toDomain() {
        return AuditLog.restore(
            id,
            actorId,
            action,
            targetType,
            targetId,
            beforeJson,
            afterJson,
            timestamp,
            traceId
        );
    }
}
