package com.aegispulse.api.metrics;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.metric.MetricIngestionUseCase;
import com.aegispulse.application.metric.command.IngestMetricPointsCommand;
import com.aegispulse.application.metric.result.IngestMetricPointsResult;
import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InternalMetricsIngestionController.class)
@Import(TraceIdFilter.class)
class InternalMetricsIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MetricIngestionUseCase metricIngestionUseCase;

    @Test
    @DisplayName("수집 요청 성공 시 200과 ingestedCount를 반환한다")
    void shouldIngestMetricsWhenRequestIsValid() throws Exception {
        given(metricIngestionUseCase.ingest(any()))
            .willReturn(IngestMetricPointsResult.builder().ingestedCount(2).build());

        mockMvc.perform(
            post("/api/v1/internal/metrics/points:batch")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-metric-ingest-001")
                .content(
                    """
                    {
                      "points": [
                        {
                          "serviceId": "svc_01",
                          "routeId": "rte_01",
                          "windowStart": "2026-02-27T01:02:30Z",
                          "rps": 11.0,
                          "latencyP50": 20.0,
                          "latencyP95": 50.0,
                          "status4xxRate": 0.2,
                          "status5xxRate": 0.1
                        },
                        {
                          "serviceId": "svc_01",
                          "consumerId": "csm_01",
                          "windowStart": "2026-02-27T01:02:30Z",
                          "rps": 9.0,
                          "latencyP50": 18.0,
                          "latencyP95": 45.0,
                          "status4xxRate": 0.1,
                          "status5xxRate": 0.05
                        }
                      ]
                    }
                    """
                )
        )
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-metric-ingest-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ingestedCount").value(2))
            .andExpect(jsonPath("$.traceId").value("trace-metric-ingest-001"));

        ArgumentCaptor<IngestMetricPointsCommand> commandCaptor = ArgumentCaptor.forClass(IngestMetricPointsCommand.class);
        then(metricIngestionUseCase).should().ingest(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getPoints()).hasSize(2);
    }

    @Test
    @DisplayName("필수 필드가 누락되면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(
            post("/api/v1/internal/metrics/points:batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "points",
                            List.of(
                                Map.of(
                                    "windowStart", "2026-02-27T01:02:30Z",
                                    "rps", 11.0,
                                    "latencyP50", 20.0,
                                    "latencyP95", 50.0,
                                    "status4xxRate", 0.2,
                                    "status5xxRate", 0.1
                                )
                            )
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(metricIngestionUseCase).should(never()).ingest(any());
    }

    @Test
    @DisplayName("축 조합이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenAxisCombinationIsInvalid() throws Exception {
        given(metricIngestionUseCase.ingest(any()))
            .willThrow(new AegisPulseException(ErrorCode.INVALID_REQUEST, "routeId와 consumerId는 동시에 설정할 수 없습니다."));

        mockMvc.perform(
            post("/api/v1/internal/metrics/points:batch")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-metric-invalid-axis")
                .content(
                    """
                    {
                      "points": [
                        {
                          "serviceId": "svc_01",
                          "routeId": "rte_01",
                          "consumerId": "csm_01",
                          "windowStart": "2026-02-27T01:02:30Z",
                          "rps": 11.0,
                          "latencyP50": 20.0,
                          "latencyP95": 50.0,
                          "status4xxRate": 0.2,
                          "status5xxRate": 0.1
                        }
                      ]
                    }
                    """
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId").value("trace-metric-invalid-axis"));
    }
}
