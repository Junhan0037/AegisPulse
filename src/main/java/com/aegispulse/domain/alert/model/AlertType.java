package com.aegispulse.domain.alert.model;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;

/**
 * FR-007 기본 임계치 알림 타입.
 */
public enum AlertType {
    SERVICE_5XX_RATE_HIGH,
    SERVICE_P95_LATENCY_HIGH;

    /**
     * 쿼리 파라미터 문자열을 알림 타입으로 변환한다.
     * 미입력은 null을 반환해 "전체 타입" 조회로 처리한다.
     */
    public static AlertType fromQuery(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return AlertType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "alertType은 SERVICE_5XX_RATE_HIGH, SERVICE_P95_LATENCY_HIGH 중 하나여야 합니다."
            );
        }
    }
}
