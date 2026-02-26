package com.aegispulse.api.policy;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.policy.dto.ApplyTemplatePolicyRequest;
import com.aegispulse.api.policy.dto.ApplyTemplatePolicyResponse;
import com.aegispulse.application.policy.TemplatePolicyApplyUseCase;
import com.aegispulse.application.policy.command.ApplyTemplatePolicyCommand;
import com.aegispulse.application.policy.result.ApplyTemplatePolicyResult;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 2 템플릿 정책 적용 API 컨트롤러.
 * 템플릿 타입 파싱과 입력 정규화를 수행하고 유스케이스 호출 결과를 공통 응답으로 변환한다.
 */
@RestController
@RequestMapping("/api/v1/policies/templates")
@RequiredArgsConstructor
public class PolicyTemplateCommandController {

    private final TemplatePolicyApplyUseCase templatePolicyApplyUseCase;

    @PostMapping("/{templateType}/apply")
    public ResponseEntity<ApiResponse<ApplyTemplatePolicyResponse>> applyTemplate(
        @PathVariable String templateType,
        @Valid @RequestBody ApplyTemplatePolicyRequest request,
        HttpServletRequest httpServletRequest
    ) {
        TemplateType resolvedTemplateType = parseTemplateType(templateType);
        validatePartnerRequest(resolvedTemplateType, request.getConsumerId());

        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId(request.getServiceId().trim())
            .routeId(normalizeOptionalId(request.getRouteId()))
            .consumerId(normalizeOptionalId(request.getConsumerId()))
            .templateType(resolvedTemplateType)
            .build();

        ApplyTemplatePolicyResult result = templatePolicyApplyUseCase.apply(command);
        ApplyTemplatePolicyResponse response = ApplyTemplatePolicyResponse.builder()
            .bindingId(result.getBindingId())
            .serviceId(result.getServiceId())
            .routeId(result.getRouteId())
            .templateType(result.getTemplateType())
            .version(result.getVersion())
            .appliedAt(result.getAppliedAt())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
    }

    private TemplateType parseTemplateType(String templateType) {
        try {
            // URL path 값은 소문자 입력을 허용하고 내부 enum 대문자 표준으로 변환한다.
            return TemplateType.valueOf(templateType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "templateType은 public, partner, internal 중 하나여야 합니다."
            );
        }
    }

    private String normalizeOptionalId(String rawId) {
        if (!StringUtils.hasText(rawId)) {
            return null;
        }
        return rawId.trim();
    }

    private void validatePartnerRequest(TemplateType templateType, String consumerId) {
        if (templateType == TemplateType.PARTNER && !StringUtils.hasText(consumerId)) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "partner 템플릿은 consumerId가 필수입니다.");
        }
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
