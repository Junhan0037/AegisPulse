package com.aegispulse.infra.web.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aegispulse.infra.web.config.WebMvcLoggingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(controllers = LoggingProbeTestController.class)
@Import({TraceIdFilter.class, RequestStructuredLoggingInterceptor.class, WebMvcLoggingConfig.class})
class RequestStructuredLoggingInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("요청 완료 시 traceId가 포함된 JSON 구조화 접근 로그를 출력한다")
    void shouldWriteStructuredAccessLog(CapturedOutput output) throws Exception {
        String traceId = "traceLog001";

        mockMvc.perform(get("/test/logging").header(TraceIdSupport.TRACE_ID_HEADER, traceId))
            .andExpect(status().isOk());

        String logs = output.getAll();

        assertThat(logs).contains("\"event\":\"http_access\"");
        assertThat(logs).contains("\"traceId\":\"" + traceId + "\"");
        assertThat(logs).contains("\"path\":\"/test/logging\"");
        assertThat(logs).contains("\"status\":200");
    }
}
