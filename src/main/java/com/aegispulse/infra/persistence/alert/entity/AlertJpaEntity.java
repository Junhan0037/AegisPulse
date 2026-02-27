package com.aegispulse.infra.persistence.alert.entity;

import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 JPA 엔티티.
 */
@Entity
@Table(
    name = "alerts",
    indexes = {
        @Index(name = "idx_alerts_state_triggered_at", columnList = "state, triggered_at"),
        @Index(name = "idx_alerts_target_type_triggered_at", columnList = "target_id, alert_type, triggered_at")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 60)
    private AlertType alertType;

    @Column(name = "target_id", nullable = false, length = 40)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertState state;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private Instant triggeredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Lob
    @Column(nullable = false)
    private String payload;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = "alt_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }

    public static AlertJpaEntity fromDomain(Alert alert) {
        AlertJpaEntity entity = new AlertJpaEntity();
        entity.setId(alert.getId());
        entity.setAlertType(alert.getAlertType());
        entity.setTargetId(alert.getTargetId());
        entity.setState(alert.getState());
        entity.setTriggeredAt(alert.getTriggeredAt());
        entity.setResolvedAt(alert.getResolvedAt());
        entity.setPayload(alert.getPayload());
        return entity;
    }

    public Alert toDomain() {
        return Alert.restore(
            id,
            alertType,
            targetId,
            state,
            triggeredAt,
            resolvedAt,
            payload
        );
    }
}
