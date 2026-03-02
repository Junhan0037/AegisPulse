package com.aegispulse.application.audit;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditLog;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 감사로그 기록 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
public class AuditLogWriteService implements AuditLogWriteUseCase {

    private static final String EMPTY_JSON = "{}";

    private final AuditLogRepository auditLogRepository;
    private final AuditPayloadMaskingService auditPayloadMaskingService;

    @Override
    @Transactional
    public void record(
        AuditAction action,
        AuditTargetType targetType,
        String targetId,
        String actorId,
        String traceId,
        String beforeJson,
        String afterJson
    ) {
        validateRequired(actorId, "actorId");
        validateRequired(traceId, "traceId");
        validateRequired(targetId, "targetId");

        String normalizedBefore = normalizeJson(beforeJson);
        String normalizedAfter = normalizeJson(afterJson);

        AuditLog auditLog = AuditLog.newLog(
            null,
            actorId.trim(),
            action,
            targetType,
            targetId.trim(),
            auditPayloadMaskingService.mask(normalizedBefore),
            auditPayloadMaskingService.mask(normalizedAfter),
            traceId.trim()
        );
        auditLogRepository.save(auditLog);
    }

    private String normalizeJson(String json) {
        if (!StringUtils.hasText(json)) {
            return EMPTY_JSON;
        }
        return json;
    }

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AegisPulseException(ErrorCode.INTERNAL_SERVER_ERROR, fieldName + "가 누락되어 감사로그를 저장할 수 없습니다.");
        }
    }
}
