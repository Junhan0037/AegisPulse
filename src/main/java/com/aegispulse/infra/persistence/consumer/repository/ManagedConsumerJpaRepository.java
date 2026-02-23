package com.aegispulse.infra.persistence.consumer.repository;

import com.aegispulse.infra.persistence.consumer.entity.ManagedConsumerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consumer JPA 리포지토리.
 */
public interface ManagedConsumerJpaRepository extends JpaRepository<ManagedConsumerJpaEntity, String> {

    boolean existsByName(String name);
}
