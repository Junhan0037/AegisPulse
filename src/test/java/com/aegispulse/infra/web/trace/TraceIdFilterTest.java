package com.aegispulse.infra.web.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = TraceIdEchoTestController.class)
@Import(TraceIdFilter.class)
class TraceIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("traceId 헤더가 없으면 필터가 새 traceId를 생성해 응답 헤더/바디에 전파한다")
    void shouldGenerateAndPropagateTraceIdWhenHeaderMissing() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/trace"))
            .andExpect(status().isOk())
            .andExpect(header().exists(TraceIdSupport.TRACE_ID_HEADER))
            .andReturn();

        String headerTraceId = result.getResponse().getHeader(TraceIdSupport.TRACE_ID_HEADER);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(headerTraceId).isNotBlank();
        assertThat(body.path("traceId").asText()).isEqualTo(headerTraceId);
    }

    @Test
    @DisplayName("traceId 헤더가 있으면 필터가 동일 값을 그대로 전파한다")
    void shouldReuseClientTraceIdWhenHeaderProvided() throws Exception {
        String clientTraceId = "clientTraceId001";

        mockMvc.perform(get("/test/trace").header(TraceIdSupport.TRACE_ID_HEADER, clientTraceId))
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, clientTraceId))
            .andExpect(jsonPath("$.traceId").value(clientTraceId))
            .andExpect(jsonPath("$.data.traceId").value(clientTraceId));
    }
}
