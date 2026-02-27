package com.aegispulse.application.metric;

import com.aegispulse.application.metric.command.IngestMetricPointsCommand;
import com.aegispulse.application.metric.result.IngestMetricPointsResult;

/**
 * 메트릭 수집 파이프라인 유스케이스.
 */
public interface MetricIngestionUseCase {

    IngestMetricPointsResult ingest(IngestMetricPointsCommand command);
}
