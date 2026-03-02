package com.aegispulse.application.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.audit.command.QueryAuditLogsCommand;
import com.aegispulse.application.audit.result.QueryAuditLogsResult;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.audit.repository.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogQueryService auditLogQueryService;

    @Test
    @DisplayName("감사로그 목록 조회 시 기본 limit(50)을 사용한다")
    void shouldUseDefaultLimitWhenLimitIsNull() {
        given(auditLogRepository.findRecent("admin-user-001", AuditAction.ROUTE_CREATED, AuditTargetType.ROUTE, "rte_01", 50))
            .willReturn(
                List.of(
                    AuditLog.restore(
                        "aud_01",
                        "admin-user-001",
                        AuditAction.ROUTE_CREATED,
                        AuditTargetType.ROUTE,
                        "rte_01",
                        "{}",
                        "{\"paths\":[\"/payments\"]}",
                        Instant.parse("2026-03-02T01:00:00Z"),
                        "trace-audit-service-001"
                    )
                )
            );

        QueryAuditLogsResult result = auditLogQueryService.query(
            QueryAuditLogsCommand.builder()
                .actorId("admin-user-001")
                .action(AuditAction.ROUTE_CREATED)
                .targetType(AuditTargetType.ROUTE)
                .targetId("rte_01")
                .limit(null)
                .build()
        );

        assertThat(result.getLogs()).hasSize(1);
        assertThat(result.getLogs().getFirst().getAction()).isEqualTo("ROUTE_CREATED");
        assertThat(result.getLogs().getFirst().getTargetType()).isEqualTo("ROUTE");
    }

    @Test
    @DisplayName("limit이 범위를 벗어나면 INVALID_REQUEST 예외를 던진다")
    void shouldThrowInvalidRequestWhenLimitOutOfRange() {
        assertThatThrownBy(
            () ->
                auditLogQueryService.query(
                    QueryAuditLogsCommand.builder()
                        .actorId(null)
                        .action(null)
                        .targetType(null)
                        .targetId(null)
                        .limit(201)
                        .build()
                )
        )
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            });
    }
}
