package com.aegispulse.infra.persistence.alert.repository;

import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.domain.alert.repository.AlertRepository;
import com.aegispulse.infra.persistence.alert.entity.AlertJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * 알림 저장소 포트를 JPA 구현체로 연결한다.
 */
@Repository
@RequiredArgsConstructor
public class AlertRepositoryAdapter implements AlertRepository {

    private static final List<AlertState> ACTIVE_STATES = List.of(AlertState.OPEN, AlertState.ACKED);

    private final AlertJpaRepository alertJpaRepository;

    @Override
    public Alert save(Alert alert) {
        return alertJpaRepository.save(AlertJpaEntity.fromDomain(alert)).toDomain();
    }

    @Override
    public Optional<Alert> findById(String alertId) {
        return alertJpaRepository.findById(alertId).map(AlertJpaEntity::toDomain);
    }

    @Override
    public Optional<Alert> findLatestByTargetAndType(String targetId, AlertType alertType) {
        return alertJpaRepository
            .findTopByTargetIdAndAlertTypeOrderByTriggeredAtDesc(targetId, alertType)
            .map(AlertJpaEntity::toDomain);
    }

    @Override
    public Optional<Alert> findActiveByTargetAndType(String targetId, AlertType alertType) {
        return alertJpaRepository
            .findTopByTargetIdAndAlertTypeAndStateInOrderByTriggeredAtDesc(targetId, alertType, ACTIVE_STATES)
            .map(AlertJpaEntity::toDomain);
    }

    @Override
    public List<Alert> findRecent(AlertState state, String targetId, AlertType alertType, int limit) {
        return alertJpaRepository.findByFilters(state, targetId, alertType, PageRequest.of(0, limit)).stream()
            .map(AlertJpaEntity::toDomain)
            .toList();
    }
}
