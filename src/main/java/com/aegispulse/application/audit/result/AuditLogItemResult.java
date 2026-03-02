package com.aegispulse.application.audit.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 감사로그 단건 조회 결과.
 */
@Getter
@Builder
public class AuditLogItemResult {

    private final String actorId;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String before;
    private final String after;
    private final String timestamp;
    private final String traceId;
}
