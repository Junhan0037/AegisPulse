package com.aegispulse.application.metric.result;

import lombok.Builder;
import lombok.Getter;

/**
 * Top Route 대시보드 카드용 결과.
 */
@Getter
@Builder
public class TopRouteMetricResult {

    private final String routeId;
    private final double rps;
    private final double status5xxRate;
    private final double latencyP95;
}
