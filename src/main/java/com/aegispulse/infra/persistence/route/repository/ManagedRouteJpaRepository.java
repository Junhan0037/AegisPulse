package com.aegispulse.infra.persistence.route.repository;

import com.aegispulse.infra.persistence.route.entity.ManagedRouteJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Route JPA 리포지토리.
 */
public interface ManagedRouteJpaRepository extends JpaRepository<ManagedRouteJpaEntity, String> {

    List<ManagedRouteJpaEntity> findAllByServiceId(String serviceId);
}
