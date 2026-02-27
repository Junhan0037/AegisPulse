package com.aegispulse.infra.persistence.metric.repository;

import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.infra.persistence.metric.entity.MetricPointJpaEntity;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 메트릭 저장소 포트를 JPA 구현으로 연결하는 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class MetricPointRepositoryAdapter implements MetricPointRepository {

    private final MetricPointJpaRepository metricPointJpaRepository;

    @Override
    public void upsertAll(List<MetricPoint> points) {
        for (MetricPoint point : points) {
            String routeId = normalizeAxisId(point.getRouteId());
            String consumerId = normalizeAxisId(point.getConsumerId());

            MetricPointJpaEntity entity = metricPointJpaRepository
                .findByServiceIdAndRouteIdAndConsumerIdAndWindowStart(
                    point.getServiceId(),
                    routeId,
                    consumerId,
                    point.getWindowStart()
                )
                .orElseGet(() -> MetricPointJpaEntity.fromDomain(point));

            entity.setServiceId(point.getServiceId());
            entity.setRouteId(routeId);
            entity.setConsumerId(consumerId);
            entity.setWindowStart(point.getWindowStart());
            entity.setRps(point.getRps());
            entity.setLatencyP50(point.getLatencyP50());
            entity.setLatencyP95(point.getLatencyP95());
            entity.setStatus4xxRate(point.getStatus4xxRate());
            entity.setStatus5xxRate(point.getStatus5xxRate());

            metricPointJpaRepository.save(entity);
        }
    }

    @Override
    public List<MetricPoint> findByServiceIdAndWindow(String serviceId, Instant fromInclusive, Instant toExclusive) {
        return metricPointJpaRepository
            .findAllByServiceIdAndWindowStartGreaterThanEqualAndWindowStartLessThanOrderByWindowStartAsc(
                serviceId,
                fromInclusive,
                toExclusive
            )
            .stream()
            .map(MetricPointJpaEntity::toDomain)
            .toList();
    }

    private String normalizeAxisId(String axisId) {
        return axisId == null ? "" : axisId;
    }
}
