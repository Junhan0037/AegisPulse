package com.aegispulse.api.service;

import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.service.dto.CreateServiceRequest;
import com.aegispulse.api.service.dto.CreateServiceResponse;
import com.aegispulse.application.service.ServiceRegistrationUseCase;
import com.aegispulse.application.service.command.RegisterServiceCommand;
import com.aegispulse.application.service.result.RegisterServiceResult;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 1 Service 등록 API 컨트롤러.
 * 입력 검증/응답 포맷/traceId 전파 같은 인터페이스 책임만 수행한다.
 */
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceCommandController {

    private final ServiceRegistrationUseCase serviceRegistrationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateServiceResponse>> createService(
        @Valid @RequestBody CreateServiceRequest request,
        HttpServletRequest httpServletRequest
    ) {
        RegisterServiceCommand command = RegisterServiceCommand.builder()
            .name(request.getName().trim())
            .upstreamUrl(request.getUpstreamUrl().trim())
            .environment(request.getEnvironment())
            .build();

        RegisterServiceResult result = serviceRegistrationUseCase.register(command);
        CreateServiceResponse response = CreateServiceResponse.builder()
            .serviceId(result.getServiceId())
            .name(result.getName())
            .environment(result.getEnvironment())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
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
