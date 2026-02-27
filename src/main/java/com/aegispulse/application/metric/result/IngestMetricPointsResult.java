package com.aegispulse.application.metric.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 메트릭 수집 결과.
 */
@Getter
@Builder
public class IngestMetricPointsResult {

    private final int ingestedCount;
}
