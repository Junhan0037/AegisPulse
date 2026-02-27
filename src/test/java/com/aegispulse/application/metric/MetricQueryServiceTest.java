package com.aegispulse.application.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.metric.command.QueryServiceMetricsCommand;
import com.aegispulse.application.metric.result.QueryServiceMetricsResult;
import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.model.MetricWindow;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricQueryServiceTest {

    @Mock
    private ManagedServiceRepository managedServiceRepository;

    @Mock
    private MetricPointRepository metricPointRepository;

    @InjectMocks
    private MetricQueryService metricQueryService;

    @Test
    @DisplayName("없는 서비스 조회 시 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenServiceDoesNotExist() {
        given(managedServiceRepository.existsById("svc_missing")).willReturn(false);

        QueryServiceMetricsCommand command = QueryServiceMetricsCommand.builder()
            .serviceId("svc_missing")
            .window(MetricWindow.LAST_5_MINUTES)
            .build();

        assertThatThrownBy(() -> metricQueryService.queryServiceMetrics(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });
    }

    @Test
    @DisplayName("서비스/라우트/consumer 축 집계와 topRoutes 정렬을 반환한다")
    void shouldReturnAggregatedMetricsAndTopRoutes() {
        String serviceId = "svc_01";
        given(managedServiceRepository.existsById(serviceId)).willReturn(true);
        given(metricPointRepository.findByServiceIdAndWindow(org.mockito.ArgumentMatchers.eq(serviceId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .willReturn(
                List.of(
                    MetricPoint.newPoint(serviceId, null, null, Instant.parse("2026-02-27T01:00:00Z"), 100, 50, 120, 0.5, 0.3),
                    MetricPoint.newPoint(serviceId, null, null, Instant.parse("2026-02-27T01:01:00Z"), 120, 60, 130, 0.7, 0.4),
                    MetricPoint.newPoint(serviceId, "rte_a", null, Instant.parse("2026-02-27T01:00:00Z"), 30, 40, 90, 0.2, 0.1),
                    MetricPoint.newPoint(serviceId, "rte_a", null, Instant.parse("2026-02-27T01:01:00Z"), 35, 42, 95, 0.2, 0.12),
                    MetricPoint.newPoint(serviceId, "rte_b", null, Instant.parse("2026-02-27T01:00:00Z"), 80, 70, 170, 1.1, 0.9),
                    MetricPoint.newPoint(serviceId, null, "csm_a", Instant.parse("2026-02-27T01:00:00Z"), 40, 55, 140, 0.6, 0.2),
                    MetricPoint.newPoint(serviceId, null, "csm_b", Instant.parse("2026-02-27T01:00:00Z"), 70, 65, 200, 0.8, 0.5)
                )
            );

        QueryServiceMetricsResult result = metricQueryService.queryServiceMetrics(
            QueryServiceMetricsCommand.builder()
                .serviceId(serviceId)
                .window(MetricWindow.LAST_5_MINUTES)
                .build()
        );

        assertThat(result.getServiceId()).isEqualTo(serviceId);
        assertThat(result.getWindow()).isEqualTo("5m");
        assertThat(result.getServiceMetrics().getRps()).isEqualTo(110.0);
        assertThat(result.getRouteMetrics()).hasSize(2);
        assertThat(result.getConsumerMetrics()).hasSize(2);
        assertThat(result.getTopRoutes()).hasSize(2);
        assertThat(result.getTopRoutes().getFirst().getRouteId()).isEqualTo("rte_b");
    }

    @Test
    @DisplayName("서비스 축 데이터가 없으면 라우트 축으로 서비스 메트릭을 대체한다")
    void shouldFallbackToRouteAxisWhenServiceAxisMissing() {
        String serviceId = "svc_01";
        given(managedServiceRepository.existsById(serviceId)).willReturn(true);
        given(metricPointRepository.findByServiceIdAndWindow(org.mockito.ArgumentMatchers.eq(serviceId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .willReturn(
                List.of(
                    MetricPoint.newPoint(serviceId, "rte_a", null, Instant.parse("2026-02-27T01:00:00Z"), 50, 40, 80, 0.2, 0.1),
                    MetricPoint.newPoint(serviceId, "rte_b", null, Instant.parse("2026-02-27T01:00:00Z"), 70, 50, 100, 0.3, 0.2)
                )
            );

        QueryServiceMetricsResult result = metricQueryService.queryServiceMetrics(
            QueryServiceMetricsCommand.builder()
                .serviceId(serviceId)
                .window(MetricWindow.LAST_1_HOUR)
                .build()
        );

        assertThat(result.getServiceMetrics().getRps()).isEqualTo(60.0);
        assertThat(result.getTopRoutes()).hasSize(2);
    }
}
