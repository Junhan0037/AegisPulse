package com.aegispulse.api.metrics.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Top Route 응답 모델.
 */
@Getter
@Builder
public class TopRouteMetricResponse {

    private final String routeId;
    private final double rps;
    private final double status5xxRate;
    private final double latencyP95;
}
