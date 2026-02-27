package com.aegispulse.domain.alert.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FR-007 알림 도메인 모델.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Alert {

    private final String id;
    private final AlertType alertType;
    private final String targetId;
    private final AlertState state;
    private final Instant triggeredAt;
    private final Instant resolvedAt;
    private final String payload;

    /**
     * 신규 OPEN 알림을 생성한다.
     */
    public static Alert newOpenAlert(String id, AlertType alertType, String targetId, Instant triggeredAt, String payload) {
        return Alert.builder()
            .id(id)
            .alertType(alertType)
            .targetId(targetId)
            .state(AlertState.OPEN)
            .triggeredAt(triggeredAt)
            .resolvedAt(null)
            .payload(payload)
            .build();
    }

    /**
     * 영속화 계층 복원을 위한 팩토리 메서드.
     */
    public static Alert restore(
        String id,
        AlertType alertType,
        String targetId,
        AlertState state,
        Instant triggeredAt,
        Instant resolvedAt,
        String payload
    ) {
        return Alert.builder()
            .id(id)
            .alertType(alertType)
            .targetId(targetId)
            .state(state)
            .triggeredAt(triggeredAt)
            .resolvedAt(resolvedAt)
            .payload(payload)
            .build();
    }

    /**
     * 운영자가 알림을 인지(ACK)한 상태로 전이한다.
     */
    public Alert ack() {
        if (state != AlertState.OPEN) {
            throw new IllegalStateException("OPEN 상태에서만 ACK 가능합니다.");
        }
        return Alert.builder()
            .id(id)
            .alertType(alertType)
            .targetId(targetId)
            .state(AlertState.ACKED)
            .triggeredAt(triggeredAt)
            .resolvedAt(resolvedAt)
            .payload(payload)
            .build();
    }

    /**
     * 임계치 정상화 시 RESOLVED로 전이한다.
     * ACKED 상태도 함께 종료 가능해야 운영 흐름이 단순해진다.
     */
    public Alert resolve(Instant resolvedAt, String payload) {
        if (state == AlertState.RESOLVED) {
            throw new IllegalStateException("이미 RESOLVED 상태입니다.");
        }
        return Alert.builder()
            .id(id)
            .alertType(alertType)
            .targetId(targetId)
            .state(AlertState.RESOLVED)
            .triggeredAt(triggeredAt)
            .resolvedAt(resolvedAt)
            .payload(payload)
            .build();
    }
}
