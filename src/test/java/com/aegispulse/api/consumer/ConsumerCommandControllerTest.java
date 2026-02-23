package com.aegispulse.api.consumer;

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
import com.aegispulse.application.consumer.ConsumerRegistrationUseCase;
import com.aegispulse.application.consumer.command.RegisterConsumerCommand;
import com.aegispulse.application.consumer.result.RegisterConsumerResult;
import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(controllers = ConsumerCommandController.class)
@Import(TraceIdFilter.class)
class ConsumerCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConsumerRegistrationUseCase consumerRegistrationUseCase;

    @Test
    @DisplayName("Consumer 생성 성공 시 201과 공통 응답 포맷을 반환한다")
    void shouldCreateConsumerWhenRequestIsValid() throws Exception {
        given(consumerRegistrationUseCase.register(any()))
            .willReturn(
                RegisterConsumerResult.builder()
                    .consumerId("csm_01JABCXYZ")
                    .name("partner-client-a")
                    .type("PARTNER")
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/consumers")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-consumer-001")
                .content(
                    """
                    {
                      "name": "partner-client-a",
                      "type": "PARTNER"
                    }
                    """
                )
        )
            .andExpect(status().isCreated())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-consumer-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.consumerId").value("csm_01JABCXYZ"))
            .andExpect(jsonPath("$.data.name").value("partner-client-a"))
            .andExpect(jsonPath("$.data.type").value("PARTNER"))
            .andExpect(jsonPath("$.traceId").value("trace-consumer-001"));

        ArgumentCaptor<RegisterConsumerCommand> commandCaptor = ArgumentCaptor.forClass(RegisterConsumerCommand.class);
        then(consumerRegistrationUseCase).should().register(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getName()).isEqualTo("partner-client-a");
    }

    @Test
    @DisplayName("name 형식이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenNamePatternIsInvalid() throws Exception {
        mockMvc.perform(
            post("/api/v1/consumers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "Partner-Client-A",
                            "type", "PARTNER"
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(consumerRegistrationUseCase).should(never()).register(any());
    }

    @Test
    @DisplayName("유효하지 않은 type이면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenTypeIsInvalid() throws Exception {
        mockMvc.perform(
            post("/api/v1/consumers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "partner-client-a",
                      "type": "PUBLIC"
                    }
                    """
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(consumerRegistrationUseCase).should(never()).register(any());
    }

    @Test
    @DisplayName("name 중복이면 409 CONSUMER_DUPLICATED를 반환한다")
    void shouldReturnConflictWhenConsumerIsDuplicated() throws Exception {
        given(consumerRegistrationUseCase.register(any()))
            .willThrow(
                new AegisPulseException(
                    ErrorCode.CONSUMER_DUPLICATED,
                    "consumer name already exists"
                )
            );

        mockMvc.perform(
            post("/api/v1/consumers")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-consumer-dup")
                .content(
                    """
                    {
                      "name": "partner-client-a",
                      "type": "PARTNER"
                    }
                    """
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CONSUMER_DUPLICATED"))
            .andExpect(jsonPath("$.error.message").value("consumer name already exists"))
            .andExpect(jsonPath("$.error.traceId").value("trace-consumer-dup"));
    }
}
