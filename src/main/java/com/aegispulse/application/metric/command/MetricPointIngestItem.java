package com.aegispulse.application.metric.command;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 수집 API가 전달하는 메트릭 포인트 입력 항목.
 */
@Getter
@Builder
public class MetricPointIngestItem {

    private final String serviceId;
    private final String routeId;
    private final String consumerId;
    private final Instant windowStart;
    private final double rps;
    private final double latencyP50;
    private final double latencyP95;
    private final double status4xxRate;
    private final double status5xxRate;
}
