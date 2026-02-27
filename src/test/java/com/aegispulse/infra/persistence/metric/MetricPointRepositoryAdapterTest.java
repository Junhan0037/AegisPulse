package com.aegispulse.infra.persistence.metric;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.infra.persistence.metric.repository.MetricPointRepositoryAdapter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MetricPointRepositoryAdapter.class)
class MetricPointRepositoryAdapterTest {

    @Autowired
    private MetricPointRepository metricPointRepository;

    @Test
    @DisplayName("동일 자연키 upsert 시 기존 레코드를 갱신한다")
    void shouldUpdateExistingRecordWhenNaturalKeyMatches() {
        Instant windowStart = Instant.parse("2026-02-27T01:00:00Z");
        metricPointRepository.upsertAll(
            List.of(
                MetricPoint.newPoint("svc_01", "rte_01", null, windowStart, 10, 50, 120, 0.2, 0.1)
            )
        );
        metricPointRepository.upsertAll(
            List.of(
                MetricPoint.newPoint("svc_01", "rte_01", null, windowStart, 20, 70, 180, 0.5, 0.4)
            )
        );

        List<MetricPoint> points = metricPointRepository.findByServiceIdAndWindow(
            "svc_01",
            Instant.parse("2026-02-27T00:55:00Z"),
            Instant.parse("2026-02-27T01:05:00Z")
        );

        assertThat(points).hasSize(1);
        assertThat(points.getFirst().getRps()).isEqualTo(20);
        assertThat(points.getFirst().getLatencyP95()).isEqualTo(180);
        assertThat(points.getFirst().getStatus5xxRate()).isEqualTo(0.4);
    }

    @Test
    @DisplayName("조회 윈도우 범위에 포함되는 포인트만 반환한다")
    void shouldReturnOnlyPointsInWindow() {
        metricPointRepository.upsertAll(
            List.of(
                MetricPoint.newPoint("svc_02", null, null, Instant.parse("2026-02-27T01:00:00Z"), 10, 20, 30, 0.1, 0.05),
                MetricPoint.newPoint("svc_02", null, null, Instant.parse("2026-02-27T01:10:00Z"), 12, 22, 35, 0.12, 0.07)
            )
        );

        List<MetricPoint> points = metricPointRepository.findByServiceIdAndWindow(
            "svc_02",
            Instant.parse("2026-02-27T00:59:00Z"),
            Instant.parse("2026-02-27T01:05:00Z")
        );

        assertThat(points).hasSize(1);
        assertThat(points.getFirst().getWindowStart()).isEqualTo(Instant.parse("2026-02-27T01:00:00Z"));
    }
}
