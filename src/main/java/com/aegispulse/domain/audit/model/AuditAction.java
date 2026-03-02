package com.aegispulse.domain.audit.model;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;

/**
 * 감사로그 액션 타입.
 */
public enum AuditAction {
    ROUTE_CREATED,
    TEMPLATE_APPLIED,
    TEMPLATE_APPLY_FAILED,
    TEMPLATE_ROLLED_BACK,
    TEMPLATE_ROLLBACK_FAILED;

    /**
     * 쿼리 파라미터 문자열을 액션 enum으로 변환한다.
     * 미입력은 null을 반환해 전체 액션 조회로 처리한다.
     */
    public static AuditAction fromQuery(String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            return null;
        }
        try {
            return AuditAction.valueOf(rawAction.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "action은 ROUTE_CREATED, TEMPLATE_APPLIED, TEMPLATE_APPLY_FAILED, TEMPLATE_ROLLED_BACK, TEMPLATE_ROLLBACK_FAILED 중 하나여야 합니다."
            );
        }
    }
}
