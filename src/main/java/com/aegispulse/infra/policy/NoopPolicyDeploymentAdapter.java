package com.aegispulse.infra.policy;

import com.aegispulse.application.policy.PolicyDeploymentPort;
import com.aegispulse.domain.policy.model.PolicyBinding;
import org.springframework.stereotype.Component;

/**
 * Stage 2에서는 외부 게이트웨이 연동 대신 no-op으로 정책 적용 포트를 연결한다.
 */
@Component
public class NoopPolicyDeploymentAdapter implements PolicyDeploymentPort {

    @Override
    public void apply(PolicyBinding policyBinding) {
        // Kong 연동 전 단계에서는 저장된 스냅샷을 성공으로 간주한다.
    }
}
