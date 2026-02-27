package com.aegispulse.api.alert;

import com.aegispulse.api.alert.dto.AcknowledgeAlertResponse;
import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.application.alert.AlertAcknowledgeUseCase;
import com.aegispulse.application.alert.command.AcknowledgeAlertCommand;
import com.aegispulse.application.alert.result.AcknowledgeAlertResult;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-007 알림 상태 전이(ACK) API 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertCommandController {

    private final AlertAcknowledgeUseCase alertAcknowledgeUseCase;

    @PatchMapping("/{alertId}/ack")
    public ResponseEntity<ApiResponse<AcknowledgeAlertResponse>> acknowledgeAlert(
        @PathVariable String alertId,
        HttpServletRequest httpServletRequest
    ) {
        if (!StringUtils.hasText(alertId)) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "alertId는 필수입니다.");
        }

        AcknowledgeAlertResult result = alertAcknowledgeUseCase.acknowledge(
            AcknowledgeAlertCommand.builder()
                .alertId(alertId.trim())
                .build()
        );
        AcknowledgeAlertResponse response = AcknowledgeAlertResponse.builder()
            .alertId(result.getAlertId())
            .state(result.getState())
            .build();

        return ResponseEntity.ok(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
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
