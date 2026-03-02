package com.aegispulse.domain.audit.model;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;

/**
 * 감사로그 대상 타입.
 */
public enum AuditTargetType {
    ROUTE,
    TEMPLATE_POLICY;

    /**
     * 쿼리 파라미터 문자열을 대상 타입 enum으로 변환한다.
     * 미입력은 null을 반환해 전체 대상 조회로 처리한다.
     */
    public static AuditTargetType fromQuery(String rawTargetType) {
        if (rawTargetType == null || rawTargetType.isBlank()) {
            return null;
        }
        try {
            return AuditTargetType.valueOf(rawTargetType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "targetType은 ROUTE, TEMPLATE_POLICY 중 하나여야 합니다."
            );
        }
    }
}
