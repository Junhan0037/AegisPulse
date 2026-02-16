package com.aegispulse.application.policy;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.domain.policy.model.AuthPolicy;
import com.aegispulse.domain.policy.model.AuthType;
import com.aegispulse.domain.policy.model.TemplatePolicyProfile;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.domain.policy.model.TrafficPolicy;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * PRD 섹션 8 템플릿 정의를 정책 프로파일로 고정 매핑한다.
 * Stage 2-1에서는 정책 적용/저장은 제외하고, 타입별 정책 결정 로직만 캡슐화한다.
 */
@Component
public class DefaultTemplatePolicyMapper implements TemplatePolicyMapper {

    private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 100;
    private static final int INTERNAL_RATE_LIMIT_PER_MINUTE = 500;
    private static final int DEFAULT_TIMEOUT_CONNECT_MS = 1_000;
    private static final int DEFAULT_TIMEOUT_READ_MS = 3_000;
    private static final int DEFAULT_TIMEOUT_WRITE_MS = 3_000;
    private static final int DEFAULT_RETRY_COUNT = 2;
    private static final int DEFAULT_RETRY_BACKOFF_MS = 100;
    private static final int PARTNER_PAYLOAD_MAX_BYTES = 1_048_576;

    @Override
    public TemplatePolicyProfile map(TemplateType templateType) {
        if (templateType == null) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "templateType은 필수입니다.");
        }

        return switch (templateType) {
            case PUBLIC -> publicProfile();
            case PARTNER -> partnerProfile();
            case INTERNAL -> internalProfile();
        };
    }

    private TemplatePolicyProfile publicProfile() {
        return TemplatePolicyProfile.of(
            TemplateType.PUBLIC,
            defaultTrafficPolicy(),
            AuthPolicy.of(AuthType.NONE, false, List.of(), null),
            true
        );
    }

    private TemplatePolicyProfile partnerProfile() {
        return TemplatePolicyProfile.of(
            TemplateType.PARTNER,
            defaultTrafficPolicy(),
            AuthPolicy.of(AuthType.API_KEY_REQUIRED, true, List.of(), PARTNER_PAYLOAD_MAX_BYTES),
            true
        );
    }

    private TemplatePolicyProfile internalProfile() {
        // internal 템플릿은 rate limit 완화(500 req/min)와 내부 헤더 규칙을 기본 적용한다.
        return TemplatePolicyProfile.of(
            TemplateType.INTERNAL,
            TrafficPolicy.of(
                INTERNAL_RATE_LIMIT_PER_MINUTE,
                DEFAULT_TIMEOUT_CONNECT_MS,
                DEFAULT_TIMEOUT_READ_MS,
                DEFAULT_TIMEOUT_WRITE_MS,
                DEFAULT_RETRY_COUNT,
                DEFAULT_RETRY_BACKOFF_MS
            ),
            AuthPolicy.of(AuthType.JWT_REQUIRED, false, List.of("X-Internal-Client"), null),
            true
        );
    }

    private TrafficPolicy defaultTrafficPolicy() {
        return TrafficPolicy.of(
            DEFAULT_RATE_LIMIT_PER_MINUTE,
            DEFAULT_TIMEOUT_CONNECT_MS,
            DEFAULT_TIMEOUT_READ_MS,
            DEFAULT_TIMEOUT_WRITE_MS,
            DEFAULT_RETRY_COUNT,
            DEFAULT_RETRY_BACKOFF_MS
        );
    }
}
