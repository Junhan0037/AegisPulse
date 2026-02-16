package com.aegispulse.domain.policy.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FR-004 트래픽 보호 정책(rate-limit/timeout/retry) 값 객체.
 * 생성 시점에 PRD 허용 범위를 강제해 잘못된 정책 구성이 저장되지 않도록 방어한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TrafficPolicy {

    private static final int MIN_RATE_LIMIT_PER_MINUTE = 10;
    private static final int MAX_RATE_LIMIT_PER_MINUTE = 10_000;
    private static final int MIN_TIMEOUT_MS = 500;
    private static final int MAX_TIMEOUT_MS = 10_000;
    private static final int MIN_RETRY_COUNT = 0;
    private static final int MAX_RETRY_COUNT = 5;

    private final int rateLimitPerMinute;
    private final int timeoutConnectMs;
    private final int timeoutReadMs;
    private final int timeoutWriteMs;
    private final int retryCount;
    private final int retryBackoffMs;

    public static TrafficPolicy of(
        int rateLimitPerMinute,
        int timeoutConnectMs,
        int timeoutReadMs,
        int timeoutWriteMs,
        int retryCount,
        int retryBackoffMs
    ) {
        validateRange("rateLimitPerMinute", rateLimitPerMinute, MIN_RATE_LIMIT_PER_MINUTE, MAX_RATE_LIMIT_PER_MINUTE);
        validateRange("timeoutConnectMs", timeoutConnectMs, MIN_TIMEOUT_MS, MAX_TIMEOUT_MS);
        validateRange("timeoutReadMs", timeoutReadMs, MIN_TIMEOUT_MS, MAX_TIMEOUT_MS);
        validateRange("timeoutWriteMs", timeoutWriteMs, MIN_TIMEOUT_MS, MAX_TIMEOUT_MS);
        validateRange("retryCount", retryCount, MIN_RETRY_COUNT, MAX_RETRY_COUNT);
        validateMin("retryBackoffMs", retryBackoffMs, 1);

        return TrafficPolicy.builder()
            .rateLimitPerMinute(rateLimitPerMinute)
            .timeoutConnectMs(timeoutConnectMs)
            .timeoutReadMs(timeoutReadMs)
            .timeoutWriteMs(timeoutWriteMs)
            .retryCount(retryCount)
            .retryBackoffMs(retryBackoffMs)
            .build();
    }

    private static void validateRange(String fieldName, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + "는 " + min + "~" + max + " 범위여야 합니다.");
        }
    }

    private static void validateMin(String fieldName, int value, int min) {
        if (value < min) {
            throw new IllegalArgumentException(fieldName + "는 " + min + " 이상이어야 합니다.");
        }
    }
}
