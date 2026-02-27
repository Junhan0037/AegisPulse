package com.aegispulse.infra.persistence.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.infra.persistence.alert.entity.AlertJpaEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertJpaEntityMappingTest {

    @Test
    @DisplayName("도메인 -> 엔티티 -> 도메인 매핑 시 핵심 필드가 유지된다")
    void shouldKeepValuesAcrossDomainEntityMapping() {
        Alert domain = Alert.restore(
            "alt_01",
            AlertType.SERVICE_P95_LATENCY_HIGH,
            "svc_01",
            AlertState.ACKED,
            Instant.parse("2026-02-27T01:00:00Z"),
            null,
            "{\"observedValue\":950}"
        );

        AlertJpaEntity entity = AlertJpaEntity.fromDomain(domain);
        Alert restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo("alt_01");
        assertThat(restored.getAlertType()).isEqualTo(AlertType.SERVICE_P95_LATENCY_HIGH);
        assertThat(restored.getTargetId()).isEqualTo("svc_01");
        assertThat(restored.getState()).isEqualTo(AlertState.ACKED);
        assertThat(restored.getTriggeredAt()).isEqualTo(Instant.parse("2026-02-27T01:00:00Z"));
    }
}
