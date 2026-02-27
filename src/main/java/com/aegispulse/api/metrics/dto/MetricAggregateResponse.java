package com.aegispulse.api.metrics.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 메트릭 공통 응답 모델.
 */
@Getter
@Builder
public class MetricAggregateResponse {

    private final double rps;
    private final double status4xxRate;
    private final double status5xxRate;
    private final double latencyP50;
    private final double latencyP95;
}
