package com.aegispulse.infra.persistence.route.repository;

import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.repository.ManagedRouteRepository;
import com.aegispulse.infra.persistence.route.entity.ManagedRouteJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Route 도메인 저장소 포트를 JPA 구현체로 연결하는 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class ManagedRouteRepositoryAdapter implements ManagedRouteRepository {

    private final ManagedRouteJpaRepository managedRouteJpaRepository;

    @Override
    public List<ManagedRoute> findAllByServiceId(String serviceId) {
        return managedRouteJpaRepository.findAllByServiceId(serviceId).stream()
            .map(ManagedRouteJpaEntity::toDomain)
            .toList();
    }

    @Override
    public ManagedRoute save(ManagedRoute route) {
        return managedRouteJpaRepository.save(ManagedRouteJpaEntity.fromDomain(route)).toDomain();
    }
}
