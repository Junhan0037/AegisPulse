package com.aegispulse.api.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.alert.AlertAcknowledgeUseCase;
import com.aegispulse.application.alert.command.AcknowledgeAlertCommand;
import com.aegispulse.application.alert.result.AcknowledgeAlertResult;
import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AlertCommandController.class)
@Import(TraceIdFilter.class)
class AlertCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertAcknowledgeUseCase alertAcknowledgeUseCase;

    @Test
    @DisplayName("알림 ACK 성공 시 200과 상태를 반환한다")
    void shouldAcknowledgeAlert() throws Exception {
        given(alertAcknowledgeUseCase.acknowledge(any()))
            .willReturn(
                AcknowledgeAlertResult.builder()
                    .alertId("alt_01")
                    .state("ACKED")
                    .build()
            );

        mockMvc.perform(
            patch("/api/v1/alerts/alt_01/ack")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-alert-ack-001")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-alert-ack-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.alertId").value("alt_01"))
            .andExpect(jsonPath("$.data.state").value("ACKED"))
            .andExpect(jsonPath("$.traceId").value("trace-alert-ack-001"));

        ArgumentCaptor<AcknowledgeAlertCommand> commandCaptor = ArgumentCaptor.forClass(AcknowledgeAlertCommand.class);
        then(alertAcknowledgeUseCase).should().acknowledge(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getAlertId()).isEqualTo("alt_01");
    }

    @Test
    @DisplayName("없는 알림이면 404 ALERT_NOT_FOUND를 반환한다")
    void shouldReturnNotFoundWhenAlertDoesNotExist() throws Exception {
        given(alertAcknowledgeUseCase.acknowledge(any()))
            .willThrow(new AegisPulseException(ErrorCode.ALERT_NOT_FOUND, "요청한 알림을 찾을 수 없습니다."));

        mockMvc.perform(
            patch("/api/v1/alerts/alt_missing/ack")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-alert-ack-not-found")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ALERT_NOT_FOUND"))
            .andExpect(jsonPath("$.error.traceId").value("trace-alert-ack-not-found"));
    }

    @Test
    @DisplayName("상태 충돌이면 409 ALERT_STATE_CONFLICT를 반환한다")
    void shouldReturnConflictWhenStateTransitionIsInvalid() throws Exception {
        given(alertAcknowledgeUseCase.acknowledge(any()))
            .willThrow(new AegisPulseException(ErrorCode.ALERT_STATE_CONFLICT, "OPEN 상태에서만 ACK 가능합니다."));

        mockMvc.perform(
            patch("/api/v1/alerts/alt_acked/ack")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-alert-ack-conflict")
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ALERT_STATE_CONFLICT"))
            .andExpect(jsonPath("$.error.traceId").value("trace-alert-ack-conflict"));
    }
}
