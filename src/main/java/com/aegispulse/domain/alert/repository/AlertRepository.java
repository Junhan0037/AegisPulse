package com.aegispulse.domain.alert.repository;

import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import java.util.List;
import java.util.Optional;

/**
 * 알림 저장소 추상화.
 */
public interface AlertRepository {

    /**
     * 알림을 저장한다.
     */
    Alert save(Alert alert);

    /**
     * 알림 ID 단건 조회.
     */
    Optional<Alert> findById(String alertId);

    /**
     * 동일 타깃/타입의 최신 알림을 조회한다.
     */
    Optional<Alert> findLatestByTargetAndType(String targetId, AlertType alertType);

    /**
     * OPEN/ACKED 상태의 활성 알림을 조회한다.
     */
    Optional<Alert> findActiveByTargetAndType(String targetId, AlertType alertType);

    /**
     * 상태/타깃/타입 필터로 최근 알림을 조회한다.
     */
    List<Alert> findRecent(AlertState state, String targetId, AlertType alertType, int limit);
}
