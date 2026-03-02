package com.aegispulse.domain.audit.model;

import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FR-008 감사로그 도메인 모델.
 * 누가(actor)/무엇(action)/어디(target)/변경 전후(before/after)/추적(traceId)을 보존한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLog {

    private final String id;
    private final String actorId;
    private final AuditAction action;
    private final AuditTargetType targetType;
    private final String targetId;
    private final String beforeJson;
    private final String afterJson;
    private final Instant timestamp;
    private final String traceId;

    /**
     * 신규 감사로그를 생성한다.
     * id는 DB 생성 전략을 허용하기 위해 null을 전달할 수 있다.
     */
    public static AuditLog newLog(
        String id,
        String actorId,
        AuditAction action,
        AuditTargetType targetType,
        String targetId,
        String beforeJson,
        String afterJson,
        String traceId
    ) {
        return AuditLog.builder()
            .id(id)
            .actorId(requireText(actorId, "actorId"))
            .action(Objects.requireNonNull(action, "action은 필수입니다."))
            .targetType(Objects.requireNonNull(targetType, "targetType은 필수입니다."))
            .targetId(requireText(targetId, "targetId"))
            .beforeJson(requireText(beforeJson, "beforeJson"))
            .afterJson(requireText(afterJson, "afterJson"))
            .timestamp(Instant.now())
            .traceId(requireText(traceId, "traceId"))
            .build();
    }

    /**
     * 영속 계층에서 기존 감사로그를 복원한다.
     */
    public static AuditLog restore(
        String id,
        String actorId,
        AuditAction action,
        AuditTargetType targetType,
        String targetId,
        String beforeJson,
        String afterJson,
        Instant timestamp,
        String traceId
    ) {
        return AuditLog.builder()
            .id(id)
            .actorId(requireText(actorId, "actorId"))
            .action(Objects.requireNonNull(action, "action은 필수입니다."))
            .targetType(Objects.requireNonNull(targetType, "targetType은 필수입니다."))
            .targetId(requireText(targetId, "targetId"))
            .beforeJson(requireText(beforeJson, "beforeJson"))
            .afterJson(requireText(afterJson, "afterJson"))
            .timestamp(Objects.requireNonNull(timestamp, "timestamp는 필수입니다."))
            .traceId(requireText(traceId, "traceId"))
            .build();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 비어 있을 수 없습니다.");
        }
        return value;
    }
}
