package com.aegispulse.infra.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.infra.persistence.audit.entity.AuditLogJpaEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditLogJpaEntityMappingTest {

    @Test
    @DisplayName("도메인 -> 엔티티 -> 도메인 매핑 시 핵심 필드가 유지된다")
    void shouldKeepValuesAcrossDomainEntityMapping() {
        AuditLog domain = AuditLog.restore(
            "aud_01",
            "admin-user-001",
            AuditAction.ROUTE_CREATED,
            AuditTargetType.ROUTE,
            "rte_01",
            "{}",
            "{\"paths\":[\"/payments\"]}",
            Instant.parse("2026-03-02T01:00:00Z"),
            "trace-audit-map-001"
        );

        AuditLogJpaEntity entity = AuditLogJpaEntity.fromDomain(domain);
        AuditLog restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo("aud_01");
        assertThat(restored.getActorId()).isEqualTo("admin-user-001");
        assertThat(restored.getAction()).isEqualTo(AuditAction.ROUTE_CREATED);
        assertThat(restored.getTargetType()).isEqualTo(AuditTargetType.ROUTE);
        assertThat(restored.getTargetId()).isEqualTo("rte_01");
        assertThat(restored.getTraceId()).isEqualTo("trace-audit-map-001");
    }
}
