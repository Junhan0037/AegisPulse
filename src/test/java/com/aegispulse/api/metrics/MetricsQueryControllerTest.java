package com.aegispulse.api.metrics;

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

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.metric.MetricQueryUseCase;
import com.aegispulse.application.metric.command.QueryServiceMetricsCommand;
import com.aegispulse.application.metric.result.ConsumerMetricResult;
import com.aegispulse.application.metric.result.MetricAggregateResult;
import com.aegispulse.application.metric.result.QueryServiceMetricsResult;
import com.aegispulse.application.metric.result.RouteMetricResult;
import com.aegispulse.application.metric.result.TopRouteMetricResult;
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

@WebMvcTest(controllers = MetricsQueryController.class)
@Import(TraceIdFilter.class)
class MetricsQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricQueryUseCase metricQueryUseCase;

    @Test
    @DisplayName("메트릭 조회 성공 시 200과 공통 응답 포맷을 반환한다")
    void shouldReturnMetricsWhenRequestIsValid() throws Exception {
        given(metricQueryUseCase.queryServiceMetrics(any()))
            .willReturn(
                QueryServiceMetricsResult.builder()
                    .serviceId("svc_01")
                    .window("1h")
                    .serviceMetrics(
                        MetricAggregateResult.builder()
                            .rps(123.4)
                            .status4xxRate(0.4)
                            .status5xxRate(0.2)
                            .latencyP50(45)
                            .latencyP95(120)
                            .build()
                    )
                    .routeMetrics(
                        List.of(
                            RouteMetricResult.builder()
                                .routeId("rte_a")
                                .metrics(
                                    MetricAggregateResult.builder()
                                        .rps(40)
                                        .status4xxRate(0.2)
                                        .status5xxRate(0.1)
                                        .latencyP50(30)
                                        .latencyP95(80)
                                        .build()
                                )
                                .build()
                        )
                    )
                    .consumerMetrics(
                        List.of(
                            ConsumerMetricResult.builder()
                                .consumerId("csm_a")
                                .metrics(
                                    MetricAggregateResult.builder()
                                        .rps(25)
                                        .status4xxRate(0.3)
                                        .status5xxRate(0.2)
                                        .latencyP50(40)
                                        .latencyP95(90)
                                        .build()
                                )
                                .build()
                        )
                    )
                    .topRoutes(
                        List.of(
                            TopRouteMetricResult.builder()
                                .routeId("rte_a")
                                .rps(40)
                                .status5xxRate(0.1)
                                .latencyP95(80)
                                .build()
                        )
                    )
                    .build()
            );

        mockMvc.perform(
            get("/api/v1/metrics/services/svc_01")
                .param("window", "1h")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-metric-001")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-metric-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.serviceId").value("svc_01"))
            .andExpect(jsonPath("$.data.window").value("1h"))
            .andExpect(jsonPath("$.data.service.rps").value(123.4))
            .andExpect(jsonPath("$.data.topRoutes[0].routeId").value("rte_a"))
            .andExpect(jsonPath("$.traceId").value("trace-metric-001"));

        ArgumentCaptor<QueryServiceMetricsCommand> commandCaptor = ArgumentCaptor.forClass(QueryServiceMetricsCommand.class);
        then(metricQueryUseCase).should().queryServiceMetrics(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getServiceId()).isEqualTo("svc_01");
        Assertions.assertThat(commandCaptor.getValue().getWindow().getQueryValue()).isEqualTo("1h");
    }

    @Test
    @DisplayName("지원하지 않는 window 값이면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenWindowIsInvalid() throws Exception {
        mockMvc.perform(
            get("/api/v1/metrics/services/svc_01")
                .param("window", "30m")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(metricQueryUseCase).should(never()).queryServiceMetrics(any());
    }

    @Test
    @DisplayName("서비스가 없으면 404 RESOURCE_NOT_FOUND를 반환한다")
    void shouldReturnNotFoundWhenServiceDoesNotExist() throws Exception {
        given(metricQueryUseCase.queryServiceMetrics(any()))
            .willThrow(new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다."));

        mockMvc.perform(
            get("/api/v1/metrics/services/svc_missing")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-metric-not-found")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.traceId").value("trace-metric-not-found"));
    }
}
