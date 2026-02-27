package com.aegispulse.application.metric.command;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 메트릭 일괄 수집 요청 커맨드.
 */
@Getter
@Builder
public class IngestMetricPointsCommand {

    private final List<MetricPointIngestItem> points;
}
