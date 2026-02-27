package com.aegispulse.infra.persistence.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.domain.alert.repository.AlertRepository;
import com.aegispulse.infra.persistence.alert.repository.AlertRepositoryAdapter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AlertRepositoryAdapter.class)
class AlertRepositoryAdapterTest {

    @Autowired
    private AlertRepository alertRepository;

    @Test
    @DisplayName("같은 서비스/타입의 최신 알림을 조회한다")
    void shouldFindLatestByTargetAndType() {
        Alert first = alertRepository.save(
            Alert.newOpenAlert(
                null,
                AlertType.SERVICE_5XX_RATE_HIGH,
                "svc_01",
                Instant.parse("2026-02-27T01:00:00Z"),
                "{\"observedValue\":2.1}"
            )
        );
        alertRepository.save(
            Alert.newOpenAlert(
                null,
                AlertType.SERVICE_5XX_RATE_HIGH,
                "svc_01",
                Instant.parse("2026-02-27T01:05:00Z"),
                "{\"observedValue\":2.9}"
            )
        );

        Alert latest = alertRepository.findLatestByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH).orElseThrow();

        assertThat(latest.getId()).isNotEqualTo(first.getId());
        assertThat(latest.getTriggeredAt()).isEqualTo(Instant.parse("2026-02-27T01:05:00Z"));
    }

    @Test
    @DisplayName("활성 상태(OPEN/ACKED) 알림만 조회한다")
    void shouldFindOnlyActiveAlert() {
        Alert resolved = Alert.newOpenAlert(
            null,
            AlertType.SERVICE_P95_LATENCY_HIGH,
            "svc_02",
            Instant.parse("2026-02-27T01:00:00Z"),
            "{\"transition\":\"OPEN\"}"
        ).resolve(Instant.parse("2026-02-27T01:04:00Z"), "{\"transition\":\"RESOLVED\"}");
        alertRepository.save(resolved);
        Alert acked = alertRepository.save(
            Alert.newOpenAlert(
                null,
                AlertType.SERVICE_P95_LATENCY_HIGH,
                "svc_02",
                Instant.parse("2026-02-27T01:06:00Z"),
                "{\"transition\":\"OPEN\"}"
            ).ack()
        );

        Alert active = alertRepository.findActiveByTargetAndType("svc_02", AlertType.SERVICE_P95_LATENCY_HIGH).orElseThrow();

        assertThat(active.getId()).isEqualTo(acked.getId());
        assertThat(active.getState()).isEqualTo(AlertState.ACKED);
    }

    @Test
    @DisplayName("상태/타깃/타입 필터와 limit을 적용해 최근 알림을 조회한다")
    void shouldFilterRecentAlerts() {
        alertRepository.save(
            Alert.newOpenAlert(
                null,
                AlertType.SERVICE_5XX_RATE_HIGH,
                "svc_03",
                Instant.parse("2026-02-27T01:00:00Z"),
                "{\"observedValue\":2.1}"
            )
        );
        alertRepository.save(
            Alert.newOpenAlert(
                null,
                AlertType.SERVICE_5XX_RATE_HIGH,
                "svc_03",
                Instant.parse("2026-02-27T01:01:00Z"),
                "{\"observedValue\":2.2}"
            ).ack()
        );
        alertRepository.save(
            Alert.newOpenAlert(
                null,
                AlertType.SERVICE_P95_LATENCY_HIGH,
                "svc_03",
                Instant.parse("2026-02-27T01:02:00Z"),
                "{\"observedValue\":900}"
            )
        );

        List<Alert> openAlerts = alertRepository.findRecent(AlertState.OPEN, "svc_03", null, 1);

        assertThat(openAlerts).hasSize(1);
        assertThat(openAlerts.getFirst().getState()).isEqualTo(AlertState.OPEN);
        assertThat(openAlerts.getFirst().getTriggeredAt()).isEqualTo(Instant.parse("2026-02-27T01:02:00Z"));
    }
}
