package com.aegispulse.application.metric.command;

import com.aegispulse.domain.metric.model.MetricWindow;
import lombok.Builder;
import lombok.Getter;

/**
 * 서비스 메트릭 조회 커맨드.
 */
@Getter
@Builder
public class QueryServiceMetricsCommand {

    private final String serviceId;
    private final MetricWindow window;
}
