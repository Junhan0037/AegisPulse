package com.aegispulse.api.service;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
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
import com.aegispulse.application.service.ServiceRegistrationUseCase;
import com.aegispulse.application.service.command.RegisterServiceCommand;
import com.aegispulse.application.service.result.RegisterServiceResult;
import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ServiceCommandController.class)
@Import(TraceIdFilter.class)
class ServiceCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceRegistrationUseCase serviceRegistrationUseCase;

    @Test
    @DisplayName("Service 등록 성공 시 201과 공통 응답 포맷을 반환한다")
    void shouldCreateServiceWhenRequestIsValid() throws Exception {
        given(serviceRegistrationUseCase.register(any()))
            .willReturn(
                RegisterServiceResult.builder()
                    .serviceId("svc_01JABCXYZ")
                    .name("partner-payment-api")
                    .environment("PROD")
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-service-001")
                .content(
                    """
                    {
                      "name": "partner-payment-api",
                      "upstreamUrl": "https://payment.internal.svc",
                      "environment": "PROD"
                    }
                    """
                )
        )
            .andExpect(status().isCreated())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-service-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.serviceId").value("svc_01JABCXYZ"))
            .andExpect(jsonPath("$.data.name").value("partner-payment-api"))
            .andExpect(jsonPath("$.data.environment").value("PROD"))
            .andExpect(jsonPath("$.traceId").value("trace-service-001"));

        ArgumentCaptor<RegisterServiceCommand> commandCaptor = ArgumentCaptor.forClass(RegisterServiceCommand.class);
        then(serviceRegistrationUseCase).should().register(commandCaptor.capture());
        // 컨트롤러에서 입력값을 trim해 비즈니스 계층에 정규화된 값으로 전달한다.
        org.assertj.core.api.Assertions.assertThat(commandCaptor.getValue().getName()).isEqualTo("partner-payment-api");
    }

    @Test
    @DisplayName("name 형식이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenNamePatternIsInvalid() throws Exception {
        mockMvc.perform(
            post("/api/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        java.util.Map.of(
                            "name", "Partner-API",
                            "upstreamUrl", "https://payment.internal.svc",
                            "environment", "PROD"
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(serviceRegistrationUseCase).should(never()).register(any());
    }

    @Test
    @DisplayName("동일 환경 name 중복이면 409 SERVICE_DUPLICATED를 반환한다")
    void shouldReturnConflictWhenServiceIsDuplicated() throws Exception {
        given(serviceRegistrationUseCase.register(any()))
            .willThrow(
                new AegisPulseException(
                    ErrorCode.SERVICE_DUPLICATED,
                    "service name already exists in this environment"
                )
            );

        mockMvc.perform(
            post("/api/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-service-dup")
                .content(
                    """
                    {
                      "name": "partner-payment-api",
                      "upstreamUrl": "https://payment.internal.svc",
                      "environment": "PROD"
                    }
                    """
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("SERVICE_DUPLICATED"))
            .andExpect(jsonPath("$.error.message").value("service name already exists in this environment"))
            .andExpect(jsonPath("$.error.traceId").value("trace-service-dup"));
    }
}
