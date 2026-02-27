package com.aegispulse.domain.metric.model;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import java.time.Duration;

/**
 * FR-006 조회 윈도우 정의.
 */
public enum MetricWindow {
    LAST_5_MINUTES("5m", Duration.ofMinutes(5)),
    LAST_1_HOUR("1h", Duration.ofHours(1)),
    LAST_24_HOURS("24h", Duration.ofHours(24));

    private final String queryValue;
    private final Duration duration;

    MetricWindow(String queryValue, Duration duration) {
        this.queryValue = queryValue;
        this.duration = duration;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public Duration getDuration() {
        return duration;
    }

    /**
     * 쿼리스트링 값을 윈도우 enum으로 변환한다.
     * 미입력(null/blank)은 기본값 5분을 사용한다.
     */
    public static MetricWindow fromQuery(String rawWindow) {
        if (rawWindow == null || rawWindow.isBlank()) {
            return LAST_5_MINUTES;
        }

        String normalized = rawWindow.trim().toLowerCase();
        for (MetricWindow window : values()) {
            if (window.queryValue.equals(normalized)) {
                return window;
            }
        }

        throw new AegisPulseException(
            ErrorCode.INVALID_REQUEST,
            "window는 5m, 1h, 24h 중 하나여야 합니다."
        );
    }
}
