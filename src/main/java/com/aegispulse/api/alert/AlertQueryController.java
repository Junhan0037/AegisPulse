package com.aegispulse.api.alert;

import com.aegispulse.api.alert.dto.AlertItemResponse;
import com.aegispulse.api.alert.dto.QueryAlertsResponse;
import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.application.alert.AlertQueryUseCase;
import com.aegispulse.application.alert.command.QueryAlertsCommand;
import com.aegispulse.application.alert.result.AlertItemResult;
import com.aegispulse.application.alert.result.QueryAlertsResult;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
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
 * FR-007 알림 조회 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertQueryController {

    private final AlertQueryUseCase alertQueryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<QueryAlertsResponse>> getAlerts(
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String serviceId,
        @RequestParam(required = false) String alertType,
        @RequestParam(required = false) Integer limit,
        HttpServletRequest httpServletRequest
    ) {
        AlertState parsedState = AlertState.fromQuery(state);
        AlertType parsedAlertType = AlertType.fromQuery(alertType);

        QueryAlertsResult result = alertQueryUseCase.query(
            QueryAlertsCommand.builder()
                .state(parsedState)
                .serviceId(normalizeOptional(serviceId))
                .alertType(parsedAlertType)
                .limit(limit)
                .build()
        );

        List<AlertItemResponse> alerts = result.getAlerts().stream()
            .map(this::toItemResponse)
            .toList();
        QueryAlertsResponse response = QueryAlertsResponse.builder()
            .alerts(alerts)
            .build();

        return ResponseEntity.ok(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
    }

    private AlertItemResponse toItemResponse(AlertItemResult item) {
        return AlertItemResponse.builder()
            .alertId(item.getAlertId())
            .alertType(item.getAlertType())
            .serviceId(item.getServiceId())
            .state(item.getState())
            .triggeredAt(item.getTriggeredAt())
            .resolvedAt(item.getResolvedAt())
            .payload(item.getPayload())
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
