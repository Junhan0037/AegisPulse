package com.aegispulse.domain.alert.model;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;

/**
 * 알림 상태.
 */
public enum AlertState {
    OPEN,
    ACKED,
    RESOLVED;

    /**
     * 쿼리 파라미터 문자열을 상태 enum으로 변환한다.
     * 미입력은 null을 반환해 전체 상태 조회로 처리한다.
     */
    public static AlertState fromQuery(String rawState) {
        if (rawState == null || rawState.isBlank()) {
            return null;
        }
        try {
            return AlertState.valueOf(rawState.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "state는 OPEN, ACKED, RESOLVED 중 하나여야 합니다."
            );
        }
    }
}
