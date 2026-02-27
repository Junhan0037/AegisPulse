package com.aegispulse.application.metric;

import com.aegispulse.application.metric.command.QueryServiceMetricsCommand;
import com.aegispulse.application.metric.result.QueryServiceMetricsResult;

/**
 * 서비스 메트릭 조회 유스케이스.
 */
public interface MetricQueryUseCase {

    QueryServiceMetricsResult queryServiceMetrics(QueryServiceMetricsCommand command);
}
