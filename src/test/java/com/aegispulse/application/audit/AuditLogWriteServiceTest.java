package com.aegispulse.application.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogWriteServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogWriteService auditLogWriteService;

    private final AuditPayloadMaskingService auditPayloadMaskingService = new AuditPayloadMaskingService(new ObjectMapper());

    @Test
    @DisplayName("감사로그 저장 시 before/after를 마스킹하고 누락값을 보정한다")
    void shouldMaskAndNormalizeBeforeAfterWhenRecordAuditLog() {
        auditLogWriteService = new AuditLogWriteService(auditLogRepository, auditPayloadMaskingService);
        given(auditLogRepository.save(any(AuditLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        auditLogWriteService.record(
            AuditAction.TEMPLATE_APPLIED,
            AuditTargetType.TEMPLATE_POLICY,
            "plb_01",
            "admin-user-001",
            "trace-audit-write-001",
            "",
            "{\"apiKey\":\"secret-value\",\"safe\":\"ok\"}"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getBeforeJson()).isEqualTo("{}");
        assertThat(saved.getAfterJson()).contains("\"apiKey\":\"***\"");
        assertThat(saved.getAfterJson()).contains("\"safe\":\"ok\"");
        assertThat(saved.getActorId()).isEqualTo("admin-user-001");
        assertThat(saved.getTraceId()).isEqualTo("trace-audit-write-001");
    }

    @Test
    @DisplayName("actorId가 비어 있으면 INTERNAL_SERVER_ERROR 예외를 던진다")
    void shouldThrowInternalServerErrorWhenActorIdIsMissing() {
        auditLogWriteService = new AuditLogWriteService(auditLogRepository, auditPayloadMaskingService);

        assertThatThrownBy(
            () ->
                auditLogWriteService.record(
                    AuditAction.ROUTE_CREATED,
                    AuditTargetType.ROUTE,
                    "rte_01",
                    " ",
                    "trace-audit-write-002",
                    "{}",
                    "{}"
                )
        )
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
            });
    }
}
