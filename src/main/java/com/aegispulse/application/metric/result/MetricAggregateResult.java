package com.aegispulse.application.metric.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 축별 메트릭 집계 결과 공통 모델.
 */
@Getter
@Builder
public class MetricAggregateResult {

    private final double rps;
    private final double status4xxRate;
    private final double status5xxRate;
    private final double latencyP50;
    private final double latencyP95;
}
