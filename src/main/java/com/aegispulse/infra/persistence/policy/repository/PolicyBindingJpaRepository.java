package com.aegispulse.infra.persistence.policy.repository;

import com.aegispulse.infra.persistence.policy.entity.PolicyBindingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PolicyBinding JPA 저장소.
 */
public interface PolicyBindingJpaRepository extends JpaRepository<PolicyBindingJpaEntity, String> {
}
