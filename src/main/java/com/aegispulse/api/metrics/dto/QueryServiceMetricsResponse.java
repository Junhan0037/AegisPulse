package com.aegispulse.api.metrics.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 서비스 메트릭 조회 응답 DTO.
 */
@Getter
@Builder
public class QueryServiceMetricsResponse {

    private final String serviceId;
    private final String window;
    private final MetricAggregateResponse service;
    private final List<RouteMetricResponse> routeMetrics;
    private final List<ConsumerMetricResponse> consumerMetrics;
    private final List<TopRouteMetricResponse> topRoutes;
}
