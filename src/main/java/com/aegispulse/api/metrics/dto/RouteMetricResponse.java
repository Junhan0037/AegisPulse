package com.aegispulse.api.metrics.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 라우트 축 메트릭 응답 모델.
 */
@Getter
@Builder
public class RouteMetricResponse {

    private final String routeId;
    private final MetricAggregateResponse metrics;
}
