package com.aegispulse.application.metric;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.metric.command.QueryServiceMetricsCommand;
import com.aegispulse.application.metric.result.ConsumerMetricResult;
import com.aegispulse.application.metric.result.MetricAggregateResult;
import com.aegispulse.application.metric.result.QueryServiceMetricsResult;
import com.aegispulse.application.metric.result.RouteMetricResult;
import com.aegispulse.application.metric.result.TopRouteMetricResult;
import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-006 메트릭 조회 집계 서비스.
 */
@Service
@RequiredArgsConstructor
public class MetricQueryService implements MetricQueryUseCase {

    private static final int TOP_ROUTES_LIMIT = 5;

    private final ManagedServiceRepository managedServiceRepository;
    private final MetricPointRepository metricPointRepository;

    @Override
    @Transactional(readOnly = true)
    public QueryServiceMetricsResult queryServiceMetrics(QueryServiceMetricsCommand command) {
        String serviceId = command.getServiceId();
        if (!managedServiceRepository.existsById(serviceId)) {
            throw new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다.");
        }

        Instant toExclusive = Instant.now();
        Instant fromInclusive = toExclusive.minus(command.getWindow().getDuration());
        List<MetricPoint> points = metricPointRepository.findByServiceIdAndWindow(serviceId, fromInclusive, toExclusive);

        List<MetricPoint> serviceAxisPoints = points.stream().filter(this::isServiceAxis).toList();
        List<MetricPoint> routeAxisPoints = points.stream().filter(this::isRouteAxis).toList();
        List<MetricPoint> consumerAxisPoints = points.stream().filter(this::isConsumerAxis).toList();

        // 수집 파이프라인이 서비스 축을 아직 보내지 않은 경우 라우트 축으로 서비스 요약을 대체한다.
        List<MetricPoint> serviceMetricsSource = resolveServiceSource(serviceAxisPoints, routeAxisPoints, consumerAxisPoints);

        Map<String, List<MetricPoint>> routeGroups = routeAxisPoints.stream()
            .collect(java.util.stream.Collectors.groupingBy(MetricPoint::getRouteId));
        Map<String, List<MetricPoint>> consumerGroups = consumerAxisPoints.stream()
            .collect(java.util.stream.Collectors.groupingBy(MetricPoint::getConsumerId));

        List<RouteMetricResult> routeMetrics = routeGroups.entrySet().stream()
            .map(entry -> RouteMetricResult.builder()
                .routeId(entry.getKey())
                .metrics(aggregate(entry.getValue()))
                .build())
            .sorted(Comparator.comparing(RouteMetricResult::getRouteId))
            .toList();

        List<ConsumerMetricResult> consumerMetrics = consumerGroups.entrySet().stream()
            .map(entry -> ConsumerMetricResult.builder()
                .consumerId(entry.getKey())
                .metrics(aggregate(entry.getValue()))
                .build())
            .sorted(Comparator.comparing(ConsumerMetricResult::getConsumerId))
            .toList();

        List<TopRouteMetricResult> topRoutes = routeMetrics.stream()
            .map(routeMetric -> TopRouteMetricResult.builder()
                .routeId(routeMetric.getRouteId())
                .rps(routeMetric.getMetrics().getRps())
                .status5xxRate(routeMetric.getMetrics().getStatus5xxRate())
                .latencyP95(routeMetric.getMetrics().getLatencyP95())
                .build())
            .sorted(
                Comparator.comparingDouble(TopRouteMetricResult::getRps).reversed()
                    .thenComparing(Comparator.comparingDouble(TopRouteMetricResult::getStatus5xxRate).reversed())
                    .thenComparing(TopRouteMetricResult::getRouteId)
            )
            .limit(TOP_ROUTES_LIMIT)
            .toList();

        return QueryServiceMetricsResult.builder()
            .serviceId(serviceId)
            .window(command.getWindow().getQueryValue())
            .serviceMetrics(aggregate(serviceMetricsSource))
            .routeMetrics(routeMetrics)
            .consumerMetrics(consumerMetrics)
            .topRoutes(topRoutes)
            .build();
    }

    private List<MetricPoint> resolveServiceSource(
        List<MetricPoint> serviceAxisPoints,
        List<MetricPoint> routeAxisPoints,
        List<MetricPoint> consumerAxisPoints
    ) {
        if (!serviceAxisPoints.isEmpty()) {
            return serviceAxisPoints;
        }
        if (!routeAxisPoints.isEmpty()) {
            return routeAxisPoints;
        }
        return consumerAxisPoints;
    }

    private MetricAggregateResult aggregate(List<MetricPoint> points) {
        if (points.isEmpty()) {
            return MetricAggregateResult.builder()
                .rps(0)
                .status4xxRate(0)
                .status5xxRate(0)
                .latencyP50(0)
                .latencyP95(0)
                .build();
        }

        double avgRps = points.stream().mapToDouble(MetricPoint::getRps).average().orElse(0);
        return MetricAggregateResult.builder()
            .rps(avgRps)
            .status4xxRate(weightedAverage(points, MetricPoint::getStatus4xxRate))
            .status5xxRate(weightedAverage(points, MetricPoint::getStatus5xxRate))
            .latencyP50(weightedAverage(points, MetricPoint::getLatencyP50))
            .latencyP95(weightedAverage(points, MetricPoint::getLatencyP95))
            .build();
    }

    private double weightedAverage(List<MetricPoint> points, java.util.function.ToDoubleFunction<MetricPoint> extractor) {
        double totalWeight = points.stream().mapToDouble(MetricPoint::getRps).sum();
        if (totalWeight <= 0) {
            return points.stream().mapToDouble(extractor).average().orElse(0);
        }

        double weightedSum = points.stream()
            .mapToDouble(point -> extractor.applyAsDouble(point) * point.getRps())
            .sum();
        return weightedSum / totalWeight;
    }

    private boolean isServiceAxis(MetricPoint point) {
        return point.getRouteId() == null && point.getConsumerId() == null;
    }

    private boolean isRouteAxis(MetricPoint point) {
        return point.getRouteId() != null && point.getConsumerId() == null;
    }

    private boolean isConsumerAxis(MetricPoint point) {
        return point.getRouteId() == null && point.getConsumerId() != null;
    }
}
