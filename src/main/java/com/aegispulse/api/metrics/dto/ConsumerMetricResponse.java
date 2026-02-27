package com.aegispulse.api.metrics.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * consumer 축 메트릭 응답 모델.
 */
@Getter
@Builder
public class ConsumerMetricResponse {

    private final String consumerId;
    private final MetricAggregateResponse metrics;
}
