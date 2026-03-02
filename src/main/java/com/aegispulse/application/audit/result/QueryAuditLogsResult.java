package com.aegispulse.application.audit.result;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 감사로그 목록 조회 결과.
 */
@Getter
@Builder
public class QueryAuditLogsResult {

    private final List<AuditLogItemResult> logs;
}
