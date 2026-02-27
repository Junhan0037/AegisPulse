package com.aegispulse.application.metric;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.metric.command.IngestMetricPointsCommand;
import com.aegispulse.application.metric.command.MetricPointIngestItem;
import com.aegispulse.application.metric.result.IngestMetricPointsResult;
import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Collector Push 입력을 메트릭 저장소로 적재하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class MetricIngestionService implements MetricIngestionUseCase {

    private final MetricPointRepository metricPointRepository;
    private final ManagedServiceRepository managedServiceRepository;

    @Override
    @Transactional
    public IngestMetricPointsResult ingest(IngestMetricPointsCommand command) {
        if (command.getPoints() == null || command.getPoints().isEmpty()) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "points는 최소 1개 이상이어야 합니다.");
        }

        List<MetricPoint> points = command.getPoints().stream()
            .map(this::toDomainPoint)
            .toList();

        metricPointRepository.upsertAll(points);
        return IngestMetricPointsResult.builder()
            .ingestedCount(points.size())
            .build();
    }

    private MetricPoint toDomainPoint(MetricPointIngestItem item) {
        String serviceId = normalizeRequired(item.getServiceId(), "serviceId는 필수입니다.");
        String routeId = normalizeOptional(item.getRouteId());
        String consumerId = normalizeOptional(item.getConsumerId());

        if (routeId != null && consumerId != null) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "routeId와 consumerId는 동시에 설정할 수 없습니다."
            );
        }
        if (!managedServiceRepository.existsById(serviceId)) {
            throw new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다.");
        }

        // 초/밀리초 오차를 제거해 동일 분 단위 natural key로 안정적으로 upsert한다.
        return MetricPoint.newPoint(
            serviceId,
            routeId,
            consumerId,
            item.getWindowStart().truncatedTo(ChronoUnit.MINUTES),
            item.getRps(),
            item.getLatencyP50(),
            item.getLatencyP95(),
            item.getStatus4xxRate(),
            item.getStatus5xxRate()
        );
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
