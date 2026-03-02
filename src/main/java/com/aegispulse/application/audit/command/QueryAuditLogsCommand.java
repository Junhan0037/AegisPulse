package com.aegispulse.application.audit.command;

import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;
import lombok.Builder;
import lombok.Getter;

/**
 * 감사로그 조회 입력 커맨드.
 */
@Getter
@Builder
public class QueryAuditLogsCommand {

    private final String actorId;
    private final AuditAction action;
    private final AuditTargetType targetType;
    private final String targetId;
    private final Integer limit;
}
