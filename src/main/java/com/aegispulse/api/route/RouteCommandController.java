package com.aegispulse.api.route;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.route.dto.CreateRouteRequest;
import com.aegispulse.api.route.dto.CreateRouteResponse;
import com.aegispulse.application.route.RouteRegistrationUseCase;
import com.aegispulse.application.route.command.RegisterRouteCommand;
import com.aegispulse.application.route.result.RegisterRouteResult;
import com.aegispulse.domain.route.model.RouteHttpMethod;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 1 Route 등록 API 컨트롤러.
 * 입력 정규화/응답 포맷/traceId 전파 등 인터페이스 책임만 수행한다.
 */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteCommandController {

    private static final String ACTOR_ID_HEADER = "X-Actor-Id";

    private final RouteRegistrationUseCase routeRegistrationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateRouteResponse>> createRoute(
        @Valid @RequestBody CreateRouteRequest request,
        @RequestHeader(value = ACTOR_ID_HEADER, required = false) String actorId,
        HttpServletRequest httpServletRequest
    ) {
        String traceId = resolveTraceId(httpServletRequest);
        RegisterRouteCommand command = RegisterRouteCommand.builder()
            .serviceId(request.getServiceId().trim())
            .paths(normalizePaths(request.getPaths()))
            .hosts(normalizeHosts(request.getHosts()))
            .methods(normalizeMethods(request.getMethods()))
            .stripPath(resolveStripPath(request.getStripPath()))
            .actorId(normalizeRequiredActorId(actorId))
            .traceId(traceId)
            .build();

        RegisterRouteResult result = routeRegistrationUseCase.register(command);
        CreateRouteResponse response = CreateRouteResponse.builder()
            .routeId(result.getRouteId())
            .serviceId(result.getServiceId())
            .paths(result.getPaths())
            .hosts(result.getHosts())
            .methods(result.getMethods())
            .stripPath(result.isStripPath())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, traceId));
    }

    private List<String> normalizePaths(List<String> paths) {
        return paths.stream()
            .map(String::trim)
            .distinct()
            .toList();
    }

    private List<String> normalizeHosts(List<String> hosts) {
        if (hosts == null) {
            return Collections.emptyList();
        }
        // host 비교는 케이스 비민감하게 처리하기 위해 소문자로 정규화한다.
        return hosts.stream()
            .map(String::trim)
            .map(value -> value.toLowerCase(Locale.ROOT))
            .distinct()
            .toList();
    }

    private List<RouteHttpMethod> normalizeMethods(List<RouteHttpMethod> methods) {
        if (methods == null) {
            return Collections.emptyList();
        }
        return methods.stream().distinct().toList();
    }

    private boolean resolveStripPath(Boolean stripPath) {
        // FR-002 기본값: stripPath 미입력 시 true.
        return stripPath == null || stripPath;
    }

    private String normalizeRequiredActorId(String actorId) {
        if (!StringUtils.hasText(actorId)) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "X-Actor-Id 헤더가 필요합니다.");
        }
        return actorId.trim();
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 필터 체인이 비정상 동작해도 응답 traceId를 보장해 클라이언트 디버깅 가능성을 유지한다.
        Object traceIdAttribute = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        return TraceIdSupport.generate();
    }
}
