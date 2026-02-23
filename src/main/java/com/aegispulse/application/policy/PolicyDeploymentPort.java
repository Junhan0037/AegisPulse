package com.aegispulse.application.policy;

import com.aegispulse.domain.policy.model.PolicyBinding;

/**
 * 템플릿 정책을 실제 실행 환경(예: API Gateway)에 반영하는 포트.
 */
public interface PolicyDeploymentPort {

    /**
     * 전달된 바인딩 스냅샷을 실행 환경에 적용한다.
     */
    void apply(PolicyBinding policyBinding);
}
