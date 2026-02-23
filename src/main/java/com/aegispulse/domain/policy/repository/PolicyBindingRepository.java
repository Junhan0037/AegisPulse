package com.aegispulse.domain.policy.repository;

import com.aegispulse.domain.policy.model.PolicyBinding;
import java.util.Optional;

/**
 * PolicyBinding 저장소 추상화.
 * 정책 적용 이력 저장 책임을 애플리케이션 계층에서 분리한다.
 */
public interface PolicyBindingRepository {

    /**
     * 정책 바인딩을 저장하고 저장된 엔티티를 반환한다.
     */
    PolicyBinding save(PolicyBinding policyBinding);

    /**
     * 서비스/라우트 조합의 최신 정책 바인딩을 조회한다.
     * routeId가 null이면 서비스 단위 정책의 최신 이력을 반환한다.
     */
    Optional<PolicyBinding> findLatest(String serviceId, String routeId);
}
