package com.aegispulse.application.audit;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.audit.command.QueryAuditLogsCommand;
import com.aegispulse.application.audit.result.AuditLogItemResult;
import com.aegispulse.application.audit.result.QueryAuditLogsResult;
import com.aegispulse.domain.audit.repository.AuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 감사로그 조회 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService implements AuditLogQueryUseCase {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public QueryAuditLogsResult query(QueryAuditLogsCommand command) {
        int limit = resolveLimit(command.getLimit());

        List<AuditLogItemResult> logs = auditLogRepository.findRecent(
                normalizeOptional(command.getActorId()),
                command.getAction(),
                command.getTargetType(),
                normalizeOptional(command.getTargetId()),
                limit
            )
            .stream()
            .map(
                log -> AuditLogItemResult.builder()
                    .actorId(log.getActorId())
                    .action(log.getAction().name())
                    .targetType(log.getTargetType().name())
                    .targetId(log.getTargetId())
                    .before(log.getBeforeJson())
                    .after(log.getAfterJson())
                    .timestamp(log.getTimestamp().toString())
                    .traceId(log.getTraceId())
                    .build()
            )
            .toList();

        return QueryAuditLogsResult.builder()
            .logs(logs)
            .build();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "limit은 1~200 범위여야 합니다.");
        }
        return limit;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
