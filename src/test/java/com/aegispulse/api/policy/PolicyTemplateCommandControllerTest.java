package com.aegispulse.api.policy;

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
import com.aegispulse.application.policy.TemplatePolicyApplyUseCase;
import com.aegispulse.application.policy.command.ApplyTemplatePolicyCommand;
import com.aegispulse.application.policy.result.ApplyTemplatePolicyResult;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.infra.web.trace.TraceIdFilter;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import java.time.Instant;
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

@WebMvcTest(controllers = PolicyTemplateCommandController.class)
@Import(TraceIdFilter.class)
class PolicyTemplateCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplatePolicyApplyUseCase templatePolicyApplyUseCase;

    @Test
    @DisplayName("템플릿 적용 성공 시 201과 공통 응답 포맷을 반환한다")
    void shouldApplyTemplateWhenRequestIsValid() throws Exception {
        given(templatePolicyApplyUseCase.apply(any()))
            .willReturn(
                ApplyTemplatePolicyResult.builder()
                    .bindingId("plb_01JABCXYZ")
                    .serviceId("svc_01JABCXYZ")
                    .routeId("rte_01JABCXYZ")
                    .templateType("PARTNER")
                    .version(1)
                    .appliedAt(Instant.parse("2026-02-20T12:30:00Z"))
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/policies/templates/partner/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-policy-001")
                .content(
                    """
                    {
                      "serviceId": " svc_01JABCXYZ ",
                      "routeId": " rte_01JABCXYZ "
                    }
                    """
                )
        )
            .andExpect(status().isCreated())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-policy-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.bindingId").value("plb_01JABCXYZ"))
            .andExpect(jsonPath("$.data.serviceId").value("svc_01JABCXYZ"))
            .andExpect(jsonPath("$.data.routeId").value("rte_01JABCXYZ"))
            .andExpect(jsonPath("$.data.templateType").value("PARTNER"))
            .andExpect(jsonPath("$.data.version").value(1))
            .andExpect(jsonPath("$.traceId").value("trace-policy-001"));

        ArgumentCaptor<ApplyTemplatePolicyCommand> commandCaptor = ArgumentCaptor.forClass(ApplyTemplatePolicyCommand.class);
        then(templatePolicyApplyUseCase).should().apply(commandCaptor.capture());
        // path/body 입력을 정규화해 유스케이스로 전달하는지 검증한다.
        Assertions.assertThat(commandCaptor.getValue().getServiceId()).isEqualTo("svc_01JABCXYZ");
        Assertions.assertThat(commandCaptor.getValue().getRouteId()).isEqualTo("rte_01JABCXYZ");
        Assertions.assertThat(commandCaptor.getValue().getTemplateType()).isEqualTo(TemplateType.PARTNER);
    }

    @Test
    @DisplayName("templateType이 허용값이 아니면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenTemplateTypeIsInvalid() throws Exception {
        mockMvc.perform(
            post("/api/v1/policies/templates/unknown/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ"
                    }
                    """
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(templatePolicyApplyUseCase).should(never()).apply(any());
    }

    @Test
    @DisplayName("serviceId가 존재하지 않으면 404 RESOURCE_NOT_FOUND를 반환한다")
    void shouldReturnNotFoundWhenServiceDoesNotExist() throws Exception {
        given(templatePolicyApplyUseCase.apply(any()))
            .willThrow(new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다."));

        mockMvc.perform(
            post("/api/v1/policies/templates/public/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-policy-not-found")
                .content(
                    """
                    {
                      "serviceId": "svc_missing"
                    }
                    """
                )
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.traceId").value("trace-policy-not-found"));
    }

    @Test
    @DisplayName("routeId가 비어 있으면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenRouteIdIsBlank() throws Exception {
        mockMvc.perform(
            post("/api/v1/policies/templates/internal/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ",
                      "routeId": "   "
                    }
                    """
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        then(templatePolicyApplyUseCase).should(never()).apply(any());
    }

    @Test
    @DisplayName("routeId가 서비스에 속하지 않으면 404 RESOURCE_NOT_FOUND를 반환한다")
    void shouldReturnNotFoundWhenRouteDoesNotBelongToService() throws Exception {
        given(templatePolicyApplyUseCase.apply(any()))
            .willThrow(new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 라우트를 찾을 수 없습니다."));

        mockMvc.perform(
            post("/api/v1/policies/templates/internal/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-policy-route-not-found")
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ",
                      "routeId": "rte_missing"
                    }
                    """
                )
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.traceId").value("trace-policy-route-not-found"));
    }
}
