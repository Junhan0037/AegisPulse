package com.aegispulse.api.audit.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 감사로그 목록 조회 응답 DTO.
 */
@Getter
@Builder
public class QueryAuditLogsResponse {

    private final List<AuditLogItemResponse> logs;
}
