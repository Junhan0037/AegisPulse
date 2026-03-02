package com.aegispulse.api.audit;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aegispulse.application.audit.AuditLogQueryUseCase;
import com.aegispulse.application.audit.command.QueryAuditLogsCommand;
import com.aegispulse.application.audit.result.AuditLogItemResult;
import com.aegispulse.application.audit.result.QueryAuditLogsResult;
import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditLogQueryController.class)
@Import(TraceIdFilter.class)
class AuditLogQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogQueryUseCase auditLogQueryUseCase;

    @Test
    @DisplayName("감사로그 조회 성공 시 200과 공통 응답 포맷을 반환한다")
    void shouldReturnAuditLogsWhenRequestIsValid() throws Exception {
        given(auditLogQueryUseCase.query(any()))
            .willReturn(
                QueryAuditLogsResult.builder()
                    .logs(
                        List.of(
                            AuditLogItemResult.builder()
                                .actorId("admin-user-001")
                                .action("TEMPLATE_APPLIED")
                                .targetType("TEMPLATE_POLICY")
                                .targetId("plb_01")
                                .before("{}")
                                .after("{\"auth\":\"***\"}")
                                .timestamp("2026-03-02T01:00:00Z")
                                .traceId("trace-audit-001")
                                .build()
                        )
                    )
                    .build()
            );

        mockMvc.perform(
            get("/api/v1/audit-logs")
                .param("actorId", "admin-user-001")
                .param("action", "template_applied")
                .param("targetType", "template_policy")
                .param("targetId", "plb_01")
                .param("limit", "20")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-audit-001")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-audit-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.logs[0].action").value("TEMPLATE_APPLIED"))
            .andExpect(jsonPath("$.data.logs[0].targetId").value("plb_01"))
            .andExpect(jsonPath("$.traceId").value("trace-audit-001"));

        ArgumentCaptor<QueryAuditLogsCommand> commandCaptor = ArgumentCaptor.forClass(QueryAuditLogsCommand.class);
        then(auditLogQueryUseCase).should().query(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getActorId()).isEqualTo("admin-user-001");
        Assertions.assertThat(commandCaptor.getValue().getAction().name()).isEqualTo("TEMPLATE_APPLIED");
        Assertions.assertThat(commandCaptor.getValue().getTargetType().name()).isEqualTo("TEMPLATE_POLICY");
        Assertions.assertThat(commandCaptor.getValue().getTargetId()).isEqualTo("plb_01");
        Assertions.assertThat(commandCaptor.getValue().getLimit()).isEqualTo(20);
    }

    @Test
    @DisplayName("지원하지 않는 action 값이면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenActionIsInvalid() throws Exception {
        mockMvc.perform(
            get("/api/v1/audit-logs")
                .param("action", "unknown_action")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(auditLogQueryUseCase).should(never()).query(any());
    }
}
