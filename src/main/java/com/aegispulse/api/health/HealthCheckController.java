package com.aegispulse.api.health;

import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.health.dto.HealthCheckResponse;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 0 기본 헬스체크 엔드포인트.
 * 서비스 부팅 상태를 API 규약(ApiResponse)으로 반환한다.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    @GetMapping("/health")
    public ApiResponse<HealthCheckResponse> health(HttpServletRequest request) {
        String traceId = resolveTraceId(request);

        HealthCheckResponse response = HealthCheckResponse.builder()
            .status("UP")
            .service("aegispulse")
            .checkedAt(Instant.now())
            .build();

        return ApiResponse.success(response, traceId);
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 필터가 traceId를 설정하지 못한 비정상 경로에서도 응답 상관관계를 보장한다.
        Object traceIdAttribute = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        return TraceIdSupport.generate();
    }
}
