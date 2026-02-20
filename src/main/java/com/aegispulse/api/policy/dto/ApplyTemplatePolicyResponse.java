package com.aegispulse.api.policy.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 템플릿 정책 적용 응답 DTO.
 */
@Getter
@Builder
public class ApplyTemplatePolicyResponse {

    private final String bindingId;
    private final String serviceId;
    private final String routeId;
    private final String templateType;
    private final int version;
    private final Instant appliedAt;
}
