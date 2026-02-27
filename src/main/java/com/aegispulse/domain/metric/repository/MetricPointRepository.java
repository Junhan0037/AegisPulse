package com.aegispulse.domain.metric.repository;

import com.aegispulse.domain.metric.model.MetricPoint;
import java.time.Instant;
import java.util.List;

/**
 * 메트릭 포인트 저장소 추상화.
 */
public interface MetricPointRepository {

    /**
     * 동일 자연키(serviceId/routeId/consumerId/windowStart) 기준으로 upsert 저장한다.
     */
    void upsertAll(List<MetricPoint> points);

    /**
     * 특정 서비스의 조회 윈도우 내 메트릭 포인트를 조회한다.
     */
    List<MetricPoint> findByServiceIdAndWindow(String serviceId, Instant fromInclusive, Instant toExclusive);
}
