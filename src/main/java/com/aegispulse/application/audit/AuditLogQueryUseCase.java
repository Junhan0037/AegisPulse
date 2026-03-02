package com.aegispulse.application.audit;

import com.aegispulse.application.audit.command.QueryAuditLogsCommand;
import com.aegispulse.application.audit.result.QueryAuditLogsResult;

/**
 * 감사로그 조회 유스케이스 계약.
 */
public interface AuditLogQueryUseCase {

    QueryAuditLogsResult query(QueryAuditLogsCommand command);
}
