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
import com.aegispulse.application.consumer.key.AuthenticateConsumerKeyUseCase;
import com.aegispulse.application.consumer.key.IssueConsumerKeyUseCase;
import com.aegispulse.application.consumer.key.command.AuthenticateConsumerKeyCommand;
import com.aegispulse.application.consumer.key.command.IssueConsumerKeyCommand;
import com.aegispulse.application.consumer.key.result.AuthenticateConsumerKeyResult;
import com.aegispulse.application.consumer.key.result.IssueConsumerKeyResult;
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

    @MockitoBean
    private IssueConsumerKeyUseCase issueConsumerKeyUseCase;

    @MockitoBean
    private AuthenticateConsumerKeyUseCase authenticateConsumerKeyUseCase;

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

    @Test
    @DisplayName("API Key 발급 성공 시 201과 1회 노출 키를 반환한다")
    void shouldIssueConsumerKeyWhenConsumerIsPartner() throws Exception {
        given(issueConsumerKeyUseCase.issue(any()))
            .willReturn(
                IssueConsumerKeyResult.builder()
                    .keyId("key_01JABCXYZ")
                    .apiKey("ak_plain_key_once")
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/consumers/csm_01JABCXYZ/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-key-issue-001")
        )
            .andExpect(status().isCreated())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-key-issue-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.keyId").value("key_01JABCXYZ"))
            .andExpect(jsonPath("$.data.apiKey").value("ak_plain_key_once"))
            .andExpect(jsonPath("$.traceId").value("trace-key-issue-001"));

        ArgumentCaptor<IssueConsumerKeyCommand> commandCaptor = ArgumentCaptor.forClass(IssueConsumerKeyCommand.class);
        then(issueConsumerKeyUseCase).should().issue(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getConsumerId()).isEqualTo("csm_01JABCXYZ");
    }

    @Test
    @DisplayName("consumerId를 유스케이스로 전달한다")
    void shouldPassConsumerIdToUseCase() throws Exception {
        given(issueConsumerKeyUseCase.issue(any()))
            .willReturn(
                IssueConsumerKeyResult.builder()
                    .keyId("key_01JTRIM")
                    .apiKey("ak_trimmed")
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/consumers/csm_trim/keys")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.keyId").value("key_01JTRIM"))
            .andExpect(jsonPath("$.data.apiKey").value("ak_trimmed"));

        ArgumentCaptor<IssueConsumerKeyCommand> commandCaptor = ArgumentCaptor.forClass(IssueConsumerKeyCommand.class);
        then(issueConsumerKeyUseCase).should().issue(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getConsumerId()).isEqualTo("csm_trim");
    }

    @Test
    @DisplayName("consumer가 없으면 404 RESOURCE_NOT_FOUND를 반환한다")
    void shouldReturnNotFoundWhenConsumerDoesNotExist() throws Exception {
        given(issueConsumerKeyUseCase.issue(any()))
            .willThrow(new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 consumer를 찾을 수 없습니다."));

        mockMvc.perform(
            post("/api/v1/consumers/csm_missing/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-key-not-found")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.traceId").value("trace-key-not-found"));
    }

    @Test
    @DisplayName("partner 타입이 아니면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenConsumerTypeIsNotPartner() throws Exception {
        given(issueConsumerKeyUseCase.issue(any()))
            .willThrow(new AegisPulseException(ErrorCode.INVALID_REQUEST, "partner 타입 consumer만 API Key를 발급할 수 있습니다."));

        mockMvc.perform(
            post("/api/v1/consumers/csm_internal/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-key-type-invalid")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId").value("trace-key-type-invalid"));
    }

    @Test
    @DisplayName("API Key 인증 성공 시 200과 인증 결과를 반환한다")
    void shouldAuthenticateConsumerKeyWhenRequestIsValid() throws Exception {
        given(authenticateConsumerKeyUseCase.authenticate(any()))
            .willReturn(
                AuthenticateConsumerKeyResult.builder()
                    .authenticated(true)
                    .consumerId("csm_01JABCXYZ")
                    .keyId("key_01JABCXYZ")
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/consumers/keys/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "ak_plain_key_once")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-key-auth-001")
                .content(
                    """
                    {
                      "serviceId": " svc_01JABCXYZ ",
                      "routeId": " rte_01JABCXYZ ",
                      "consumerId": " csm_01JABCXYZ "
                    }
                    """
                )
        )
            .andExpect(status().isOk())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-key-auth-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.authenticated").value(true))
            .andExpect(jsonPath("$.data.consumerId").value("csm_01JABCXYZ"))
            .andExpect(jsonPath("$.data.keyId").value("key_01JABCXYZ"))
            .andExpect(jsonPath("$.traceId").value("trace-key-auth-001"));

        ArgumentCaptor<AuthenticateConsumerKeyCommand> commandCaptor = ArgumentCaptor.forClass(
            AuthenticateConsumerKeyCommand.class
        );
        then(authenticateConsumerKeyUseCase).should().authenticate(commandCaptor.capture());
        Assertions.assertThat(commandCaptor.getValue().getServiceId()).isEqualTo("svc_01JABCXYZ");
        Assertions.assertThat(commandCaptor.getValue().getRouteId()).isEqualTo("rte_01JABCXYZ");
        Assertions.assertThat(commandCaptor.getValue().getConsumerId()).isEqualTo("csm_01JABCXYZ");
        Assertions.assertThat(commandCaptor.getValue().getApiKey()).isEqualTo("ak_plain_key_once");
    }

    @Test
    @DisplayName("X-API-Key 헤더가 없으면 401 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenApiKeyHeaderIsMissing() throws Exception {
        mockMvc.perform(
            post("/api/v1/consumers/keys/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ",
                      "consumerId": "csm_01JABCXYZ"
                    }
                    """
                )
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(authenticateConsumerKeyUseCase).should(never()).authenticate(any());
    }

    @Test
    @DisplayName("폐기된 키면 403 FORBIDDEN을 반환한다")
    void shouldReturnForbiddenWhenApiKeyIsRevoked() throws Exception {
        given(authenticateConsumerKeyUseCase.authenticate(any()))
            .willThrow(new AegisPulseException(ErrorCode.FORBIDDEN, "폐기된 API Key입니다."));

        mockMvc.perform(
            post("/api/v1/consumers/keys/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "ak_revoked")
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-key-auth-revoked")
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ",
                      "consumerId": "csm_01JABCXYZ"
                    }
                    """
                )
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.error.traceId").value("trace-key-auth-revoked"));
    }
}
