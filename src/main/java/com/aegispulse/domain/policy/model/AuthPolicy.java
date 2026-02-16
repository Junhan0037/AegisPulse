package com.aegispulse.domain.policy.model;

import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 템플릿 인증 정책 값 객체.
 * 템플릿별 인증 요구사항(API Key/JWT/무인증)과 옵션 필드를 함께 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthPolicy {

    private final AuthType authType;
    private final boolean ipAllowlistEnabled;
    private final List<String> requiredHeaders;
    private final Integer payloadMaxBytes;

    public static AuthPolicy of(
        AuthType authType,
        boolean ipAllowlistEnabled,
        List<String> requiredHeaders,
        Integer payloadMaxBytes
    ) {
        Objects.requireNonNull(authType, "authType은 필수입니다.");
        Objects.requireNonNull(requiredHeaders, "requiredHeaders는 null일 수 없습니다.");
        if (payloadMaxBytes != null && payloadMaxBytes < 1) {
            throw new IllegalArgumentException("payloadMaxBytes는 1 이상이어야 합니다.");
        }

        return AuthPolicy.builder()
            .authType(authType)
            .ipAllowlistEnabled(ipAllowlistEnabled)
            .requiredHeaders(List.copyOf(requiredHeaders))
            .payloadMaxBytes(payloadMaxBytes)
            .build();
    }
}
