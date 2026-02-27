package com.aegispulse.application.metric.result;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 서비스 메트릭 조회 결과.
 */
@Getter
@Builder
public class QueryServiceMetricsResult {

    private final String serviceId;
    private final String window;
    private final MetricAggregateResult serviceMetrics;
    private final List<RouteMetricResult> routeMetrics;
    private final List<ConsumerMetricResult> consumerMetrics;
    private final List<TopRouteMetricResult> topRoutes;
}
