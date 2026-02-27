package com.aegispulse.infra.persistence.alert.repository;

import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.infra.persistence.alert.entity.AlertJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 알림 JPA 리포지토리.
 */
public interface AlertJpaRepository extends JpaRepository<AlertJpaEntity, String> {

    Optional<AlertJpaEntity> findTopByTargetIdAndAlertTypeOrderByTriggeredAtDesc(String targetId, AlertType alertType);

    Optional<AlertJpaEntity> findTopByTargetIdAndAlertTypeAndStateInOrderByTriggeredAtDesc(
        String targetId,
        AlertType alertType,
        List<AlertState> states
    );

    @Query(
        """
        SELECT a
        FROM AlertJpaEntity a
        WHERE (:state IS NULL OR a.state = :state)
          AND (:targetId IS NULL OR a.targetId = :targetId)
          AND (:alertType IS NULL OR a.alertType = :alertType)
        ORDER BY a.triggeredAt DESC
        """
    )
    List<AlertJpaEntity> findByFilters(
        @Param("state") AlertState state,
        @Param("targetId") String targetId,
        @Param("alertType") AlertType alertType,
        Pageable pageable
    );
}
