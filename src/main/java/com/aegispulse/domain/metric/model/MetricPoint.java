package com.aegispulse.domain.metric.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FR-006 메트릭 집계 포인트 도메인 모델.
 * 수집 파이프라인이 적재한 서비스/라우트/consumer 축의 집계 단위를 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MetricPoint {

    private final String id;
    private final String serviceId;
    private final String routeId;
    private final String consumerId;
    private final Instant windowStart;
    private final double rps;
    private final double latencyP50;
    private final double latencyP95;
    private final double status4xxRate;
    private final double status5xxRate;

    /**
     * 신규 메트릭 포인트를 생성한다.
     * id는 영속화 계층에서 부여되므로 null로 시작한다.
     */
    public static MetricPoint newPoint(
        String serviceId,
        String routeId,
        String consumerId,
        Instant windowStart,
        double rps,
        double latencyP50,
        double latencyP95,
        double status4xxRate,
        double status5xxRate
    ) {
        return MetricPoint.builder()
            .id(null)
            .serviceId(serviceId)
            .routeId(routeId)
            .consumerId(consumerId)
            .windowStart(windowStart)
            .rps(rps)
            .latencyP50(latencyP50)
            .latencyP95(latencyP95)
            .status4xxRate(status4xxRate)
            .status5xxRate(status5xxRate)
            .build();
    }

    /**
     * 영속화 계층에서 도메인 객체를 복원한다.
     */
    public static MetricPoint restore(
        String id,
        String serviceId,
        String routeId,
        String consumerId,
        Instant windowStart,
        double rps,
        double latencyP50,
        double latencyP95,
        double status4xxRate,
        double status5xxRate
    ) {
        return MetricPoint.builder()
            .id(id)
            .serviceId(serviceId)
            .routeId(routeId)
            .consumerId(consumerId)
            .windowStart(windowStart)
            .rps(rps)
            .latencyP50(latencyP50)
            .latencyP95(latencyP95)
            .status4xxRate(status4xxRate)
            .status5xxRate(status5xxRate)
            .build();
    }
}
