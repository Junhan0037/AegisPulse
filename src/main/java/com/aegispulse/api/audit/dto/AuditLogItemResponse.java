package com.aegispulse.api.audit.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 감사로그 단건 응답 DTO.
 */
@Getter
@Builder
public class AuditLogItemResponse {

    private final String actorId;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String before;
    private final String after;
    private final String timestamp;
    private final String traceId;
}
