package com.aegispulse.api.audit;

import com.aegispulse.api.audit.dto.AuditLogItemResponse;
import com.aegispulse.api.audit.dto.QueryAuditLogsResponse;
import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.application.audit.AuditLogQueryUseCase;
import com.aegispulse.application.audit.command.QueryAuditLogsCommand;
import com.aegispulse.application.audit.result.AuditLogItemResult;
import com.aegispulse.application.audit.result.QueryAuditLogsResult;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-008 감사로그 조회 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogQueryController {

    private final AuditLogQueryUseCase auditLogQueryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<QueryAuditLogsResponse>> getAuditLogs(
        @RequestParam(required = false) String actorId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String targetId,
        @RequestParam(required = false) Integer limit,
        HttpServletRequest httpServletRequest
    ) {
        AuditAction parsedAction = AuditAction.fromQuery(action);
        AuditTargetType parsedTargetType = AuditTargetType.fromQuery(targetType);

        QueryAuditLogsResult result = auditLogQueryUseCase.query(
            QueryAuditLogsCommand.builder()
                .actorId(normalizeOptional(actorId))
                .action(parsedAction)
                .targetType(parsedTargetType)
                .targetId(normalizeOptional(targetId))
                .limit(limit)
                .build()
        );

        List<AuditLogItemResponse> logs = result.getLogs().stream()
            .map(this::toItemResponse)
            .toList();

        QueryAuditLogsResponse response = QueryAuditLogsResponse.builder()
            .logs(logs)
            .build();

        return ResponseEntity.ok(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
    }

    private AuditLogItemResponse toItemResponse(AuditLogItemResult item) {
        return AuditLogItemResponse.builder()
            .actorId(item.getActorId())
            .action(item.getAction())
            .targetType(item.getTargetType())
            .targetId(item.getTargetId())
            .before(item.getBefore())
            .after(item.getAfter())
            .timestamp(item.getTimestamp())
            .traceId(item.getTraceId())
            .build();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 필터 체인이 비정상 동작해도 응답 traceId를 보장해 운영 디버깅 가능성을 유지한다.
        Object traceIdAttribute = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        return TraceIdSupport.generate();
    }
}
