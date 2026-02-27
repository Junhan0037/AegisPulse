package com.aegispulse.api.alert;

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

import com.aegispulse.application.alert.AlertQueryUseCase;
import com.aegispulse.application.alert.command.QueryAlertsCommand;
import com.aegispulse.application.alert.result.AlertItemResult;
import com.aegispulse.application.alert.result.QueryAlertsResult;
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

@WebMvcTest(controllers = AlertQueryController.class)
@Import(TraceIdFilter.class)
class AlertQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertQueryUseCase alertQueryUseCase;

    @Test
    @DisplayName("알림 조회 성공 시 200과 공통 응답 포맷을 반환한다")
    void shouldReturnAlertsWhenRequestIsValid() throws Exception {
        given(alertQueryUseCase.query(any()))
            .willReturn(
                QueryAlertsResult.builder()
                    .alerts(
                        List.of(
                            AlertItemResult.builder()
                                .alertId("alt_01")
                                .alertType("SERVICE_5XX_RATE_HIGH")
                                .serviceId("svc_01")
                                .state("OPEN")
                                .triggeredAt("2026-02-27T01:00:00Z")
                                .resolvedAt(null)
                                .payload("{\"observedValue\":2.8}")
                                .build()
                        )
                    )
                    .build()
            );

        mockMvc.perform(
            get("/api/v1/alerts")
                .param("state", "open")
                .param("serviceId", "svc_01")
                .param("alertType", "service_5xx_rate_high")
                .param("limit", "20")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-alert-query-001")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-alert-query-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.alerts[0].alertId").value("alt_01"))
            .andExpect(jsonPath("$.data.alerts[0].state").value("OPEN"))
            .andExpect(jsonPath("$.traceId").value("trace-alert-query-001"));

        ArgumentCaptor<QueryAlertsCommand> commandCaptor = ArgumentCaptor.forClass(QueryAlertsCommand.class);
        then(alertQueryUseCase).should().query(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getState().name()).isEqualTo("OPEN");
        Assertions.assertThat(commandCaptor.getValue().getAlertType().name()).isEqualTo("SERVICE_5XX_RATE_HIGH");
        Assertions.assertThat(commandCaptor.getValue().getLimit()).isEqualTo(20);
    }

    @Test
    @DisplayName("지원하지 않는 state 값이면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenStateIsInvalid() throws Exception {
        mockMvc.perform(
            get("/api/v1/alerts")
                .param("state", "paused")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(alertQueryUseCase).should(never()).query(any());
    }
}
