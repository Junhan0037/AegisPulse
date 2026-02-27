package com.aegispulse.application.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.metric.command.IngestMetricPointsCommand;
import com.aegispulse.application.metric.command.MetricPointIngestItem;
import com.aegispulse.application.metric.result.IngestMetricPointsResult;
import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricIngestionServiceTest {

    @Mock
    private MetricPointRepository metricPointRepository;

    @Mock
    private ManagedServiceRepository managedServiceRepository;

    @InjectMocks
    private MetricIngestionService metricIngestionService;

    @Test
    @DisplayName("routeId와 consumerId를 동시에 전달하면 INVALID_REQUEST 예외를 던진다")
    void shouldThrowInvalidRequestWhenRouteAndConsumerAxisExistTogether() {
        IngestMetricPointsCommand command = IngestMetricPointsCommand.builder()
            .points(
                List.of(
                    MetricPointIngestItem.builder()
                        .serviceId("svc_01")
                        .routeId("rte_01")
                        .consumerId("csm_01")
                        .windowStart(Instant.parse("2026-02-27T01:02:30Z"))
                        .rps(10)
                        .latencyP50(100)
                        .latencyP95(230)
                        .status4xxRate(0.2)
                        .status5xxRate(0.1)
                        .build()
                )
            )
            .build();

        assertThatThrownBy(() -> metricIngestionService.ingest(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            });

        then(metricPointRepository).should(never()).upsertAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("없는 serviceId를 전달하면 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenServiceDoesNotExist() {
        IngestMetricPointsCommand command = IngestMetricPointsCommand.builder()
            .points(
                List.of(
                    MetricPointIngestItem.builder()
                        .serviceId("svc_missing")
                        .windowStart(Instant.parse("2026-02-27T01:02:30Z"))
                        .rps(10)
                        .latencyP50(100)
                        .latencyP95(230)
                        .status4xxRate(0.2)
                        .status5xxRate(0.1)
                        .build()
                )
            )
            .build();

        given(managedServiceRepository.existsById("svc_missing")).willReturn(false);

        assertThatThrownBy(() -> metricIngestionService.ingest(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });

        then(metricPointRepository).should(never()).upsertAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("정상 입력이면 분 단위로 정규화 후 upsert하고 ingestedCount를 반환한다")
    void shouldNormalizeAndUpsertPoints() {
        IngestMetricPointsCommand command = IngestMetricPointsCommand.builder()
            .points(
                List.of(
                    MetricPointIngestItem.builder()
                        .serviceId(" svc_01 ")
                        .routeId(" rte_01 ")
                        .windowStart(Instant.parse("2026-02-27T01:02:30Z"))
                        .rps(10)
                        .latencyP50(100)
                        .latencyP95(230)
                        .status4xxRate(0.2)
                        .status5xxRate(0.1)
                        .build()
                )
            )
            .build();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);

        IngestMetricPointsResult result = metricIngestionService.ingest(command);

        assertThat(result.getIngestedCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        // 제네릭 List 캡처는 타입 소거로 인해 런타임에 raw class(List.class) 사용이 필요하다.
        ArgumentCaptor<List<MetricPoint>> pointCaptor = ArgumentCaptor.forClass(List.class);
        then(metricPointRepository).should().upsertAll(pointCaptor.capture());
        MetricPoint savedPoint = pointCaptor.getValue().getFirst();
        assertThat(savedPoint.getServiceId()).isEqualTo("svc_01");
        assertThat(savedPoint.getRouteId()).isEqualTo("rte_01");
        assertThat(savedPoint.getConsumerId()).isNull();
        assertThat(savedPoint.getWindowStart()).isEqualTo(Instant.parse("2026-02-27T01:02:30Z").truncatedTo(ChronoUnit.MINUTES));
    }
}
