package com.aegispulse.domain.policy.model;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 템플릿 단위 정책 매핑 결과 모델.
 * 템플릿 타입, 인증 정책, 트래픽 정책, 지표 수집 여부를 하나의 스냅샷처럼 다룬다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TemplatePolicyProfile {

    private final TemplateType templateType;
    private final TrafficPolicy trafficPolicy;
    private final AuthPolicy authPolicy;
    private final boolean metricsEnabled;

    public static TemplatePolicyProfile of(
        TemplateType templateType,
        TrafficPolicy trafficPolicy,
        AuthPolicy authPolicy,
        boolean metricsEnabled
    ) {
        return TemplatePolicyProfile.builder()
            .templateType(Objects.requireNonNull(templateType, "templateType은 필수입니다."))
            .trafficPolicy(Objects.requireNonNull(trafficPolicy, "trafficPolicy는 필수입니다."))
            .authPolicy(Objects.requireNonNull(authPolicy, "authPolicy는 필수입니다."))
            .metricsEnabled(metricsEnabled)
            .build();
    }
}
