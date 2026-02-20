package com.aegispulse.infra.persistence.route.repository;

import com.aegispulse.infra.persistence.route.entity.ManagedRouteJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Route JPA 리포지토리.
 */
public interface ManagedRouteJpaRepository extends JpaRepository<ManagedRouteJpaEntity, String> {

    List<ManagedRouteJpaEntity> findAllByServiceId(String serviceId);

    /**
     * routeId + serviceId 조합 존재 여부를 확인한다.
     * 템플릿 정책 적용 요청의 대상 무결성 검증에 사용한다.
     */
    boolean existsByIdAndServiceId(String routeId, String serviceId);
}
