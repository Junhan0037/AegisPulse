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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ApplyTemplatePolicyResult apply(ApplyTemplatePolicyCommand command) {
        validateServiceExists(command.getServiceId());
        validateRouteOwnership(command.getRouteId(), command.getServiceId());

        TemplatePolicyProfile profile = templatePolicyMapper.map(command.getTemplateType());
        String snapshot = serializePolicySnapshot(profile);

        PolicyBinding candidate = PolicyBinding.newBinding(
            generatePolicyBindingId(),
            command.getServiceId(),
            command.getRouteId(),
            command.getTemplateType(),
            snapshot,
            INITIAL_POLICY_VERSION
        );

        PolicyBinding saved = policyBindingRepository.save(candidate);
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
