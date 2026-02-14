package com.aegispulse.api.health;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HealthCheckController.class)
@Import(TraceIdFilter.class)
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("헬스체크 API는 공통 성공 응답 규약으로 UP 상태를 반환한다")
    void shouldReturnUpStatusWithApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists(TraceIdSupport.TRACE_ID_HEADER))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.service").value("aegispulse"))
            // Stage 0 요구사항: traceId가 항상 응답에 포함되어야 한다.
            .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
    }

    @Test
    @DisplayName("클라이언트 traceId를 전달하면 동일 값이 응답 헤더와 바디로 전파된다")
    void shouldPropagateClientTraceIdToHeaderAndBody() throws Exception {
        String traceId = "trace-health-001";

        mockMvc.perform(get("/api/v1/health").header(TraceIdSupport.TRACE_ID_HEADER, traceId))
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, traceId))
            .andExpect(jsonPath("$.traceId").value(traceId));
    }
}
