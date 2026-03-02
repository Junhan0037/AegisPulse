package com.aegispulse.api.route;

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
import com.aegispulse.application.route.RouteRegistrationUseCase;
import com.aegispulse.application.route.command.RegisterRouteCommand;
import com.aegispulse.application.route.result.RegisterRouteResult;
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

@WebMvcTest(controllers = RouteCommandController.class)
@Import(TraceIdFilter.class)
class RouteCommandControllerTest {

    private static final String ACTOR_ID_HEADER = "X-Actor-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RouteRegistrationUseCase routeRegistrationUseCase;

    @Test
    @DisplayName("Route 등록 성공 시 201과 공통 응답 포맷을 반환한다")
    void shouldCreateRouteWhenRequestIsValid() throws Exception {
        given(routeRegistrationUseCase.register(any()))
            .willReturn(
                RegisterRouteResult.builder()
                    .routeId("rte_01JABCXYZ")
                    .serviceId("svc_01JABCXYZ")
                    .paths(List.of("/payments"))
                    .hosts(List.of("api.partner.com"))
                    .methods(List.of("GET", "POST"))
                    .stripPath(true)
                    .build()
            );

        mockMvc.perform(
            post("/api/v1/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-route-001")
                .header(ACTOR_ID_HEADER, "admin-user-001")
                .content(
                    """
                    {
                      "serviceId": " svc_01JABCXYZ ",
                      "paths": ["/payments", "/payments"],
                      "hosts": ["API.PARTNER.COM", "api.partner.com"],
                      "methods": ["GET", "POST", "GET"]
                    }
                    """
                )
        )
            .andExpect(status().isCreated())
            .andExpect(header().string(TraceIdSupport.TRACE_ID_HEADER, "trace-route-001"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.routeId").value("rte_01JABCXYZ"))
            .andExpect(jsonPath("$.data.serviceId").value("svc_01JABCXYZ"))
            .andExpect(jsonPath("$.data.stripPath").value(true))
            .andExpect(jsonPath("$.traceId").value("trace-route-001"));

        ArgumentCaptor<RegisterRouteCommand> commandCaptor = ArgumentCaptor.forClass(RegisterRouteCommand.class);
        then(routeRegistrationUseCase).should().register(commandCaptor.capture());
        RegisterRouteCommand captured = commandCaptor.getValue();
        // 컨트롤러에서 중복 제거/정규화를 적용한 뒤 유스케이스로 전달한다.
        Assertions.assertThat(captured.getServiceId()).isEqualTo("svc_01JABCXYZ");
        Assertions.assertThat(captured.getHosts()).containsExactly("api.partner.com");
        Assertions.assertThat(captured.getMethods()).hasSize(2);
        Assertions.assertThat(captured.isStripPath()).isTrue();
        Assertions.assertThat(captured.getActorId()).isEqualTo("admin-user-001");
        Assertions.assertThat(captured.getTraceId()).isEqualTo("trace-route-001");
    }

    @Test
    @DisplayName("허용되지 않은 HTTP Method가 포함되면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenMethodIsNotAllowed() throws Exception {
        mockMvc.perform(
            post("/api/v1/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ACTOR_ID_HEADER, "admin-user-001")
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "serviceId", "svc_01JABCXYZ",
                            "paths", List.of("/payments"),
                            "methods", List.of("TRACE")
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.traceId", not(blankOrNullString())));

        then(routeRegistrationUseCase).should(never()).register(any());
    }

    @Test
    @DisplayName("serviceId가 존재하지 않으면 404 RESOURCE_NOT_FOUND를 반환한다")
    void shouldReturnNotFoundWhenServiceDoesNotExist() throws Exception {
        given(routeRegistrationUseCase.register(any()))
            .willThrow(new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다."));

        mockMvc.perform(
            post("/api/v1/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-route-not-found")
                .header(ACTOR_ID_HEADER, "admin-user-001")
                .content(
                    """
                    {
                      "serviceId": "svc_missing",
                      "paths": ["/payments"]
                    }
                    """
                )
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.traceId").value("trace-route-not-found"));
    }

    @Test
    @DisplayName("충돌하는 라우트면 409 ROUTE_CONFLICT를 반환한다")
    void shouldReturnConflictWhenRouteConflictOccurs() throws Exception {
        given(routeRegistrationUseCase.register(any()))
            .willThrow(new AegisPulseException(ErrorCode.ROUTE_CONFLICT, "동일 서비스 내 충돌하는 라우트입니다."));

        mockMvc.perform(
            post("/api/v1/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TraceIdSupport.TRACE_ID_HEADER, "trace-route-conflict")
                .header(ACTOR_ID_HEADER, "admin-user-001")
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ",
                      "paths": ["/payments"],
                      "methods": ["GET"],
                      "hosts": ["api.partner.com"]
                    }
                    """
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ROUTE_CONFLICT"))
            .andExpect(jsonPath("$.error.message").value("동일 서비스 내 충돌하는 라우트입니다."))
            .andExpect(jsonPath("$.error.traceId").value("trace-route-conflict"));
    }

    @Test
    @DisplayName("X-Actor-Id 헤더가 없으면 400 INVALID_REQUEST를 반환한다")
    void shouldReturnBadRequestWhenActorIdHeaderIsMissing() throws Exception {
        mockMvc.perform(
            post("/api/v1/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "serviceId": "svc_01JABCXYZ",
                      "paths": ["/payments"]
                    }
                    """
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.error.message").value("X-Actor-Id 헤더가 필요합니다."));

        then(routeRegistrationUseCase).should(never()).register(any());
    }
}
