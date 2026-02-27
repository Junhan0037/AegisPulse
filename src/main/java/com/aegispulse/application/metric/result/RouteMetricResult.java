package com.aegispulse.application.metric.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 라우트 축 집계 결과.
 */
@Getter
@Builder
public class RouteMetricResult {

    private final String routeId;
    private final MetricAggregateResult metrics;
}
