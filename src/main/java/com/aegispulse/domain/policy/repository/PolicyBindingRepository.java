package com.aegispulse.domain.policy.repository;

import com.aegispulse.domain.policy.model.PolicyBinding;

/**
 * PolicyBinding 저장소 추상화.
 * 정책 적용 이력 저장 책임을 애플리케이션 계층에서 분리한다.
 */
public interface PolicyBindingRepository {

    /**
     * 정책 바인딩을 저장하고 저장된 엔티티를 반환한다.
     */
    PolicyBinding save(PolicyBinding policyBinding);
}
