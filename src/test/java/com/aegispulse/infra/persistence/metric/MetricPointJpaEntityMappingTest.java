package com.aegispulse.infra.persistence.metric;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.infra.persistence.metric.entity.MetricPointJpaEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetricPointJpaEntityMappingTest {

    @Test
    @DisplayName("도메인 -> 엔티티 -> 도메인 매핑 시 축 ID null 처리와 필드 값이 유지된다")
    void shouldKeepValuesAcrossDomainEntityMapping() {
        MetricPoint domain = MetricPoint.newPoint(
            "svc_01",
            null,
            "csm_01",
            Instant.parse("2026-02-27T01:00:00Z"),
            12.3,
            40.5,
            90.1,
            0.5,
            0.2
        );

        MetricPointJpaEntity entity = MetricPointJpaEntity.fromDomain(domain);
        entity.setId("mtp_01");
        MetricPoint restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo("mtp_01");
        assertThat(restored.getServiceId()).isEqualTo("svc_01");
        assertThat(restored.getRouteId()).isNull();
        assertThat(restored.getConsumerId()).isEqualTo("csm_01");
        assertThat(restored.getWindowStart()).isEqualTo(Instant.parse("2026-02-27T01:00:00Z"));
        assertThat(restored.getRps()).isEqualTo(12.3);
    }
}
