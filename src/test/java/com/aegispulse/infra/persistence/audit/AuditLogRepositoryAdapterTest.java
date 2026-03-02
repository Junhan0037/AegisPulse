package com.aegispulse.infra.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.audit.repository.AuditLogRepository;
import com.aegispulse.infra.persistence.audit.repository.AuditLogRepositoryAdapter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AuditLogRepositoryAdapter.class)
class AuditLogRepositoryAdapterTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("actor/action/target 필터와 limit을 적용해 최근 감사로그를 조회한다")
    void shouldFilterRecentAuditLogs() {
        auditLogRepository.save(
            AuditLog.restore(
                "aud_01",
                "admin-user-001",
                AuditAction.ROUTE_CREATED,
                AuditTargetType.ROUTE,
                "rte_01",
                "{}",
                "{\"paths\":[\"/payments\"]}",
                Instant.parse("2026-03-02T01:00:00Z"),
                "trace-audit-repo-001"
            )
        );

        auditLogRepository.save(
            AuditLog.restore(
                "aud_02",
                "admin-user-001",
                AuditAction.ROUTE_CREATED,
                AuditTargetType.ROUTE,
                "rte_01",
                "{}",
                "{\"paths\":[\"/refunds\"]}",
                Instant.parse("2026-03-02T01:10:00Z"),
                "trace-audit-repo-002"
            )
        );

        auditLogRepository.save(
            AuditLog.restore(
                "aud_03",
                "admin-user-002",
                AuditAction.TEMPLATE_APPLIED,
                AuditTargetType.TEMPLATE_POLICY,
                "plb_01",
                "{}",
                "{\"templateType\":\"PARTNER\"}",
                Instant.parse("2026-03-02T01:20:00Z"),
                "trace-audit-repo-003"
            )
        );

        List<AuditLog> logs = auditLogRepository.findRecent(
            "admin-user-001",
            AuditAction.ROUTE_CREATED,
            AuditTargetType.ROUTE,
            "rte_01",
            1
        );

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getId()).isEqualTo("aud_02");
        assertThat(logs.getFirst().getTimestamp()).isEqualTo(Instant.parse("2026-03-02T01:10:00Z"));
    }
}
