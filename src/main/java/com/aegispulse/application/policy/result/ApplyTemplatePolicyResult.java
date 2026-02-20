package com.aegispulse.application.policy.result;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 템플릿 정책 적용 유스케이스 출력 모델.
 */
@Getter
@Builder
public class ApplyTemplatePolicyResult {

    private final String bindingId;
    private final String serviceId;
    private final String routeId;
    private final String templateType;
    private final int version;
    private final Instant appliedAt;
}
