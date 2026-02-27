package com.aegispulse.infra.persistence.metric.repository;

import com.aegispulse.infra.persistence.metric.entity.MetricPointJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 메트릭 포인트 JPA 리포지토리.
 */
public interface MetricPointJpaRepository extends JpaRepository<MetricPointJpaEntity, String> {

    Optional<MetricPointJpaEntity> findByServiceIdAndRouteIdAndConsumerIdAndWindowStart(
        String serviceId,
        String routeId,
        String consumerId,
        Instant windowStart
    );

    List<MetricPointJpaEntity> findAllByServiceIdAndWindowStartGreaterThanEqualAndWindowStartLessThanOrderByWindowStartAsc(
        String serviceId,
        Instant fromInclusive,
        Instant toExclusive
    );
}
