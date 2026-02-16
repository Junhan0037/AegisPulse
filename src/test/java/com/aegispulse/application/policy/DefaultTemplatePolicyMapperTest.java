package com.aegispulse.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.domain.policy.model.AuthType;
import com.aegispulse.domain.policy.model.TemplatePolicyProfile;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.domain.policy.model.TrafficPolicy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultTemplatePolicyMapperTest {

    private final TemplatePolicyMapper templatePolicyMapper = new DefaultTemplatePolicyMapper();

    @Test
    @DisplayName("public 템플릿은 무인증 + 기본 트래픽 보호 정책으로 매핑된다")
    void shouldMapPublicTemplatePolicy() {
        TemplatePolicyProfile profile = templatePolicyMapper.map(TemplateType.PUBLIC);

        assertThat(profile.getTemplateType()).isEqualTo(TemplateType.PUBLIC);
        assertThat(profile.isMetricsEnabled()).isTrue();
        assertThat(profile.getAuthPolicy().getAuthType()).isEqualTo(AuthType.NONE);
        assertThat(profile.getAuthPolicy().isIpAllowlistEnabled()).isFalse();
        assertThat(profile.getAuthPolicy().getRequiredHeaders()).isEmpty();
        assertThat(profile.getAuthPolicy().getPayloadMaxBytes()).isNull();
        assertDefaultTrafficPolicy(profile.getTrafficPolicy(), 100);
    }

    @Test
    @DisplayName("partner 템플릿은 API Key + allowlist 옵션 + 1MB payload 제한으로 매핑된다")
    void shouldMapPartnerTemplatePolicy() {
        TemplatePolicyProfile profile = templatePolicyMapper.map(TemplateType.PARTNER);

        assertThat(profile.getTemplateType()).isEqualTo(TemplateType.PARTNER);
        assertThat(profile.getAuthPolicy().getAuthType()).isEqualTo(AuthType.API_KEY_REQUIRED);
        assertThat(profile.getAuthPolicy().isIpAllowlistEnabled()).isTrue();
        assertThat(profile.getAuthPolicy().getPayloadMaxBytes()).isEqualTo(1_048_576);
        assertThat(profile.getAuthPolicy().getRequiredHeaders()).isEmpty();
        assertDefaultTrafficPolicy(profile.getTrafficPolicy(), 100);
    }

    @Test
    @DisplayName("internal 템플릿은 JWT + 내부 헤더 규칙 + 완화된 rate limit으로 매핑된다")
    void shouldMapInternalTemplatePolicy() {
        TemplatePolicyProfile profile = templatePolicyMapper.map(TemplateType.INTERNAL);

        assertThat(profile.getTemplateType()).isEqualTo(TemplateType.INTERNAL);
        assertThat(profile.getAuthPolicy().getAuthType()).isEqualTo(AuthType.JWT_REQUIRED);
        assertThat(profile.getAuthPolicy().getRequiredHeaders()).containsExactly("X-Internal-Client");
        assertThat(profile.getAuthPolicy().getPayloadMaxBytes()).isNull();
        assertDefaultTrafficPolicy(profile.getTrafficPolicy(), 500);
    }

    @Test
    @DisplayName("템플릿 매핑 결과는 FR-004 범위 제약을 만족한다")
    void shouldSatisfyTrafficPolicyRangesForAllTemplates() {
        List<TemplateType> templateTypes = List.of(TemplateType.PUBLIC, TemplateType.PARTNER, TemplateType.INTERNAL);

        for (TemplateType templateType : templateTypes) {
            TrafficPolicy trafficPolicy = templatePolicyMapper.map(templateType).getTrafficPolicy();
            // Stage 2에서 사용되는 모든 기본 정책 값이 PRD 허용 범위를 넘지 않는지 검증한다.
            assertThat(trafficPolicy.getRateLimitPerMinute()).isBetween(10, 10_000);
            assertThat(trafficPolicy.getTimeoutConnectMs()).isBetween(500, 10_000);
            assertThat(trafficPolicy.getTimeoutReadMs()).isBetween(500, 10_000);
            assertThat(trafficPolicy.getTimeoutWriteMs()).isBetween(500, 10_000);
            assertThat(trafficPolicy.getRetryCount()).isBetween(0, 5);
            assertThat(trafficPolicy.getRetryBackoffMs()).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("템플릿 타입이 null이면 INVALID_REQUEST 예외를 반환한다")
    void shouldThrowInvalidRequestWhenTemplateTypeIsNull() {
        assertThatThrownBy(() -> templatePolicyMapper.map(null))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            });
    }

    private void assertDefaultTrafficPolicy(TrafficPolicy trafficPolicy, int expectedRateLimitPerMinute) {
        assertThat(trafficPolicy.getRateLimitPerMinute()).isEqualTo(expectedRateLimitPerMinute);
        assertThat(trafficPolicy.getTimeoutConnectMs()).isEqualTo(1_000);
        assertThat(trafficPolicy.getTimeoutReadMs()).isEqualTo(3_000);
        assertThat(trafficPolicy.getTimeoutWriteMs()).isEqualTo(3_000);
        assertThat(trafficPolicy.getRetryCount()).isEqualTo(2);
        assertThat(trafficPolicy.getRetryBackoffMs()).isEqualTo(100);
    }
}
