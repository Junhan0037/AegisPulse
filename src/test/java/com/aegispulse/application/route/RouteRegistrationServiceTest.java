package com.aegispulse.application.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.audit.AuditLogWriteUseCase;
import com.aegispulse.application.route.command.RegisterRouteCommand;
import com.aegispulse.application.route.policy.RouteConflictPolicy;
import com.aegispulse.application.route.result.RegisterRouteResult;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.model.RouteHttpMethod;
import com.aegispulse.domain.route.repository.ManagedRouteRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteRegistrationServiceTest {

    @Mock
    private ManagedServiceRepository managedServiceRepository;

    @Mock
    private ManagedRouteRepository managedRouteRepository;

    @Mock
    private RouteConflictPolicy routeConflictPolicy;

    @Mock
    private AuditLogWriteUseCase auditLogWriteUseCase;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RouteRegistrationService routeRegistrationService;

    @Test
    @DisplayName("serviceId가 존재하지 않으면 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenServiceDoesNotExist() {
        RegisterRouteCommand command = RegisterRouteCommand.builder()
            .serviceId("svc_missing")
            .paths(List.of("/payments"))
            .hosts(List.of())
            .methods(List.of())
            .stripPath(true)
            .actorId("admin-user-001")
            .traceId("trace-route-test-001")
            .build();

        given(managedServiceRepository.existsById("svc_missing")).willReturn(false);

        assertThatThrownBy(() -> routeRegistrationService.register(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });

        then(managedRouteRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("동일 서비스 내 충돌 시 ROUTE_CONFLICT 예외를 던진다")
    void shouldThrowConflictWhenRouteConflicts() {
        RegisterRouteCommand command = RegisterRouteCommand.builder()
            .serviceId("svc_01")
            .paths(List.of("/payments"))
            .hosts(List.of("api.partner.com"))
            .methods(List.of(RouteHttpMethod.GET))
            .stripPath(true)
            .actorId("admin-user-001")
            .traceId("trace-route-test-002")
            .build();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(managedRouteRepository.findAllByServiceId("svc_01")).willReturn(List.of());
        given(routeConflictPolicy.hasConflict(any(), any())).willReturn(true);

        assertThatThrownBy(() -> routeRegistrationService.register(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.ROUTE_CONFLICT);
            });

        then(managedRouteRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("충돌이 없으면 routeId를 생성해 저장하고 결과를 반환한다")
    void shouldSaveAndReturnRegisteredRoute() throws Exception {
        RegisterRouteCommand command = RegisterRouteCommand.builder()
            .serviceId("svc_01")
            .paths(List.of("/payments"))
            .hosts(List.of("api.partner.com"))
            .methods(List.of(RouteHttpMethod.GET, RouteHttpMethod.POST))
            .stripPath(false)
            .actorId("admin-user-001")
            .traceId("trace-route-test-003")
            .build();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(managedRouteRepository.findAllByServiceId("svc_01")).willReturn(List.of());
        given(routeConflictPolicy.hasConflict(any(), any())).willReturn(false);
        given(managedRouteRepository.save(any(ManagedRoute.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"routeId\":\"mock\"}");

        RegisterRouteResult result = routeRegistrationService.register(command);

        assertThat(result.getRouteId()).startsWith("rte_");
        assertThat(result.getRouteId()).hasSize(36);
        assertThat(result.getServiceId()).isEqualTo("svc_01");
        assertThat(result.getMethods()).containsExactly("GET", "POST");
        assertThat(result.isStripPath()).isFalse();

        ArgumentCaptor<ManagedRoute> routeCaptor = ArgumentCaptor.forClass(ManagedRoute.class);
        then(managedRouteRepository).should().save(routeCaptor.capture());
        // stripPath 입력값이 도메인 모델에 그대로 반영되는지 확인한다.
        assertThat(routeCaptor.getValue().isStripPath()).isFalse();
        then(auditLogWriteUseCase).should(times(1))
            .record(
                eq(AuditAction.ROUTE_CREATED),
                eq(AuditTargetType.ROUTE),
                eq(result.getRouteId()),
                eq("admin-user-001"),
                eq("trace-route-test-003"),
                eq("{}"),
                anyString()
            );
    }
}
