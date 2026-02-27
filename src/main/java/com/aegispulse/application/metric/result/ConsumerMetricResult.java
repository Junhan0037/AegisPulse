package com.aegispulse.application.metric.result;

import lombok.Builder;
import lombok.Getter;

/**
 * consumer 축 집계 결과.
 */
@Getter
@Builder
public class ConsumerMetricResult {

    private final String consumerId;
    private final MetricAggregateResult metrics;
}
