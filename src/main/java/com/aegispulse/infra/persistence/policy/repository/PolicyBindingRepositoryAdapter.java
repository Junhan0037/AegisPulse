package com.aegispulse.infra.persistence.policy.repository;

import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.repository.PolicyBindingRepository;
import com.aegispulse.infra.persistence.policy.entity.PolicyBindingJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * PolicyBinding 도메인 저장소 포트를 JPA 구현체로 연결한다.
 */
@Repository
@RequiredArgsConstructor
public class PolicyBindingRepositoryAdapter implements PolicyBindingRepository {

    private final PolicyBindingJpaRepository policyBindingJpaRepository;

    @Override
    public PolicyBinding save(PolicyBinding policyBinding) {
        return policyBindingJpaRepository.save(PolicyBindingJpaEntity.fromDomain(policyBinding)).toDomain();
    }
}
