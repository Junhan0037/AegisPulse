package com.aegispulse.infra.persistence.metric.entity;

import com.aegispulse.domain.metric.model.MetricPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메트릭 포인트 JPA 엔티티.
 * PRD 권장 인덱스(MetricPoint(serviceId, windowStart))를 포함한다.
 */
@Entity
@Table(
    name = "metric_points",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_metric_points_service_route_consumer_window",
            columnNames = {"service_id", "route_id", "consumer_id", "window_start"}
        )
    },
    indexes = {
        @Index(name = "idx_metric_points_service_window", columnList = "service_id, window_start"),
        @Index(name = "idx_metric_points_service_route_window", columnList = "service_id, route_id, window_start"),
        @Index(name = "idx_metric_points_service_consumer_window", columnList = "service_id, consumer_id, window_start")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetricPointJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(name = "service_id", nullable = false, length = 40)
    private String serviceId;

    @Column(name = "route_id", nullable = false, length = 40)
    private String routeId;

    @Column(name = "consumer_id", nullable = false, length = 40)
    private String consumerId;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private double rps;

    @Column(name = "latency_p50", nullable = false)
    private double latencyP50;

    @Column(name = "latency_p95", nullable = false)
    private double latencyP95;

    @Column(name = "rate_4xx", nullable = false)
    private double status4xxRate;

    @Column(name = "rate_5xx", nullable = false)
    private double status5xxRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = "mtp_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static MetricPointJpaEntity fromDomain(MetricPoint point) {
        MetricPointJpaEntity entity = new MetricPointJpaEntity();
        entity.setId(point.getId());
        entity.setServiceId(point.getServiceId());
        entity.setRouteId(normalizeAxisId(point.getRouteId()));
        entity.setConsumerId(normalizeAxisId(point.getConsumerId()));
        entity.setWindowStart(point.getWindowStart());
        entity.setRps(point.getRps());
        entity.setLatencyP50(point.getLatencyP50());
        entity.setLatencyP95(point.getLatencyP95());
        entity.setStatus4xxRate(point.getStatus4xxRate());
        entity.setStatus5xxRate(point.getStatus5xxRate());
        return entity;
    }

    public MetricPoint toDomain() {
        return MetricPoint.restore(
            id,
            serviceId,
            restoreAxisId(routeId),
            restoreAxisId(consumerId),
            windowStart,
            rps,
            latencyP50,
            latencyP95,
            status4xxRate,
            status5xxRate
        );
    }

    private static String normalizeAxisId(String axisId) {
        return axisId == null ? "" : axisId;
    }

    private static String restoreAxisId(String axisId) {
        return axisId == null || axisId.isBlank() ? null : axisId;
    }
}
