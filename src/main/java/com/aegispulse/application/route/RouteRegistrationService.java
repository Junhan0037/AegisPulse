package com.aegispulse.application.route;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.audit.AuditLogWriteUseCase;
import com.aegispulse.application.route.command.RegisterRouteCommand;
import com.aegispulse.application.route.policy.RouteConflictPolicy;
import com.aegispulse.application.route.result.RegisterRouteResult;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.repository.ManagedRouteRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Route 등록 유스케이스 구현체.
 * 서비스 존재성 검증과 충돌 검증 후 신규 Route를 저장한다.
 */
@Service
@RequiredArgsConstructor
public class RouteRegistrationService implements RouteRegistrationUseCase {

    private final ManagedServiceRepository managedServiceRepository;
    private final ManagedRouteRepository managedRouteRepository;
    private final RouteConflictPolicy routeConflictPolicy;
    private final AuditLogWriteUseCase auditLogWriteUseCase;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RegisterRouteResult register(RegisterRouteCommand command) {
        validateAuditContext(command.getActorId(), command.getTraceId());

        if (!managedServiceRepository.existsById(command.getServiceId())) {
            throw new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다.");
        }

        ManagedRoute candidate = ManagedRoute.newRoute(
            generateRouteId(),
            command.getServiceId(),
            command.getPaths(),
            command.getHosts(),
            command.getMethods(),
            command.isStripPath()
        );

        List<ManagedRoute> existingRoutes = managedRouteRepository.findAllByServiceId(command.getServiceId());
        if (routeConflictPolicy.hasConflict(candidate, existingRoutes)) {
            throw new AegisPulseException(ErrorCode.ROUTE_CONFLICT, "동일 서비스 내 충돌하는 라우트입니다.");
        }

        ManagedRoute saved = managedRouteRepository.save(candidate);
        auditLogWriteUseCase.record(
            AuditAction.ROUTE_CREATED,
            AuditTargetType.ROUTE,
            saved.getId(),
            command.getActorId(),
            command.getTraceId(),
            "{}",
            toRouteAfterJson(saved)
        );
        return RegisterRouteResult.builder()
            .routeId(saved.getId())
            .serviceId(saved.getServiceId())
            .paths(saved.getPaths())
            .hosts(saved.getHosts())
            .methods(saved.getMethods().stream().map(Enum::name).toList())
            .stripPath(saved.isStripPath())
            .build();
    }

    private String generateRouteId() {
        return "rte_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void validateAuditContext(String actorId, String traceId) {
        // 컨트롤러 검증 누락이나 내부 호출 오류가 있어도 감사로그 필수 컨텍스트 누락을 차단한다.
        if (!StringUtils.hasText(actorId) || !StringUtils.hasText(traceId)) {
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "감사로그 필수 컨텍스트(actorId/traceId)가 누락되었습니다."
            );
        }
    }

    private String toRouteAfterJson(ManagedRoute route) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeId", route.getId());
        payload.put("serviceId", route.getServiceId());
        payload.put("paths", route.getPaths());
        payload.put("hosts", route.getHosts());
        payload.put("methods", route.getMethods().stream().map(Enum::name).toList());
        payload.put("stripPath", route.isStripPath());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new AegisPulseException(ErrorCode.INTERNAL_SERVER_ERROR, "라우트 감사로그 직렬화에 실패했습니다.");
        }
    }
}
