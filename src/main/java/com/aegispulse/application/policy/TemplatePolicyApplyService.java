package com.aegispulse.application.policy;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.policy.command.ApplyTemplatePolicyCommand;
import com.aegispulse.application.policy.result.ApplyTemplatePolicyResult;
import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplatePolicyProfile;
import com.aegispulse.domain.policy.repository.PolicyBindingRepository;
import com.aegispulse.domain.route.repository.ManagedRouteRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 템플릿 정책 적용 유스케이스 구현체.
 * 대상 존재성 검증 후 정책 스냅샷을 생성해 PolicyBinding 이력을 저장한다.
 */
@Service
@RequiredArgsConstructor
public class TemplatePolicyApplyService implements TemplatePolicyApplyUseCase {

    private static final int INITIAL_POLICY_VERSION = 1;

    private final ManagedServiceRepository managedServiceRepository;
    private final ManagedRouteRepository managedRouteRepository;
    private final TemplatePolicyMapper templatePolicyMapper;
    private final PolicyBindingRepository policyBindingRepository;
    private final PolicyDeploymentPort policyDeploymentPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(noRollbackFor = AegisPulseException.class)
    public ApplyTemplatePolicyResult apply(ApplyTemplatePolicyCommand command) {
        validateServiceExists(command.getServiceId());
        validateRouteOwnership(command.getRouteId(), command.getServiceId());

        Optional<PolicyBinding> previousBinding = policyBindingRepository.findLatest(
            command.getServiceId(),
            command.getRouteId()
        );
        TemplatePolicyProfile profile = templatePolicyMapper.map(command.getTemplateType());
        String snapshot = serializePolicySnapshot(profile);
        int nextVersion = previousBinding.map(binding -> binding.getVersion() + 1).orElse(INITIAL_POLICY_VERSION);

        PolicyBinding candidate = PolicyBinding.newBinding(
            generatePolicyBindingId(),
            command.getServiceId(),
            command.getRouteId(),
            command.getTemplateType(),
            snapshot,
            nextVersion
        );

        PolicyBinding saved = policyBindingRepository.save(candidate);
        try {
            policyDeploymentPort.apply(saved);
        } catch (RuntimeException ignored) {
            rollbackWhenApplyFailed(previousBinding, saved);
        }

        return buildApplyResult(saved);
    }

    private void rollbackWhenApplyFailed(Optional<PolicyBinding> previousBinding, PolicyBinding failedBinding) {
        if (previousBinding.isEmpty()) {
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "정책 적용에 실패했습니다. 복구할 이전 스냅샷이 없습니다."
            );
        }

        PolicyBinding latest = previousBinding.get();
        PolicyBinding rollbackCandidate = PolicyBinding.newBinding(
            generatePolicyBindingId(),
            failedBinding.getServiceId(),
            failedBinding.getRouteId(),
            latest.getTemplateType(),
            latest.getPolicySnapshot(),
            failedBinding.getVersion() + 1
        );

        PolicyBinding rollbackBinding = policyBindingRepository.save(rollbackCandidate);
        try {
            policyDeploymentPort.apply(rollbackBinding);
        } catch (RuntimeException rollbackException) {
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "정책 적용 및 자동 롤백에 실패했습니다."
            );
        }

        throw new AegisPulseException(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "정책 적용에 실패하여 이전 스냅샷으로 자동 롤백했습니다."
        );
    }

    private ApplyTemplatePolicyResult buildApplyResult(PolicyBinding saved) {
        return ApplyTemplatePolicyResult.builder()
            .bindingId(saved.getId())
            .serviceId(saved.getServiceId())
            .routeId(saved.getRouteId())
            .templateType(saved.getTemplateType().name())
            .version(saved.getVersion())
            .appliedAt(saved.getCreatedAt())
            .build();
    }

    private void validateServiceExists(String serviceId) {
        if (!managedServiceRepository.existsById(serviceId)) {
            throw new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다.");
        }
    }

    private void validateRouteOwnership(String routeId, String serviceId) {
        // routeId가 지정된 요청만 라우트 소속 검증을 수행한다.
        if (routeId == null) {
            return;
        }
        if (!managedRouteRepository.existsByIdAndServiceId(routeId, serviceId)) {
            throw new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 라우트를 찾을 수 없습니다.");
        }
    }

    private String serializePolicySnapshot(TemplatePolicyProfile profile) {
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException exception) {
            // 스냅샷 직렬화 실패는 서버 내부 처리 오류로 간주한다.
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "정책 스냅샷 직렬화에 실패했습니다."
            );
        }
    }

    private String generatePolicyBindingId() {
        return "plb_" + UUID.randomUUID().toString().replace("-", "");
    }
}
