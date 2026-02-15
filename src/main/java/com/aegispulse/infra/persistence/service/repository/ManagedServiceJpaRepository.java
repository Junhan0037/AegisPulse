package com.aegispulse.infra.persistence.service.repository;

import com.aegispulse.domain.service.model.ServiceEnvironment;
import com.aegispulse.infra.persistence.service.entity.ManagedServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Service JPA 리포지토리.
 */
public interface ManagedServiceJpaRepository extends JpaRepository<ManagedServiceJpaEntity, String> {

    boolean existsByEnvironmentAndName(ServiceEnvironment environment, String name);
}
