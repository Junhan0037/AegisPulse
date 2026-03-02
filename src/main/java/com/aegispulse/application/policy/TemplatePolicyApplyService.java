package com.aegispulse.application.policy;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.audit.AuditLogWriteUseCase;
import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.repository.ManagedConsumerKeyRepository;
import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import com.aegispulse.domain.audit.model.AuditAction;
import com.aegispulse.domain.audit.model.AuditTargetType;
import com.aegispulse.application.policy.command.ApplyTemplatePolicyCommand;
import com.aegispulse.application.policy.result.ApplyTemplatePolicyResult;
import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplatePolicyProfile;
import com.aegispulse.domain.policy.model.TemplateType;
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
import org.springframework.util.StringUtils;

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
    private final ManagedConsumerRepository managedConsumerRepository;
    private final ManagedConsumerKeyRepository managedConsumerKeyRepository;
    private final TemplatePolicyMapper templatePolicyMapper;
    private final PolicyBindingRepository policyBindingRepository;
    private final PolicyDeploymentPort policyDeploymentPort;
    private final AuditLogWriteUseCase auditLogWriteUseCase;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(noRollbackFor = AegisPulseException.class)
    public ApplyTemplatePolicyResult apply(ApplyTemplatePolicyCommand command) {
        validateAuditContext(command.getActorId(), command.getTraceId());
        validateServiceExists(command.getServiceId());
        validateRouteOwnership(command.getRouteId(), command.getServiceId());
        validatePartnerTemplateRequirements(command);

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
            rollbackWhenApplyFailed(command, previousBinding, saved);
        }

        recordAudit(
            AuditAction.TEMPLATE_APPLIED,
            saved.getId(),
            command.getActorId(),
            command.getTraceId(),
            previousBinding.map(PolicyBinding::getPolicySnapshot).orElse("{}"),
            saved.getPolicySnapshot()
        );

        return buildApplyResult(saved);
    }

    private void rollbackWhenApplyFailed(
        ApplyTemplatePolicyCommand command,
        Optional<PolicyBinding> previousBinding,
        PolicyBinding failedBinding
    ) {
        recordAudit(
            AuditAction.TEMPLATE_APPLY_FAILED,
            failedBinding.getId(),
            command.getActorId(),
            command.getTraceId(),
            previousBinding.map(PolicyBinding::getPolicySnapshot).orElse("{}"),
            failedBinding.getPolicySnapshot()
        );

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
            recordAudit(
                AuditAction.TEMPLATE_ROLLBACK_FAILED,
                rollbackBinding.getId(),
                command.getActorId(),
                command.getTraceId(),
                failedBinding.getPolicySnapshot(),
                rollbackBinding.getPolicySnapshot()
            );
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "정책 적용 및 자동 롤백에 실패했습니다."
            );
        }

        recordAudit(
            AuditAction.TEMPLATE_ROLLED_BACK,
            rollbackBinding.getId(),
            command.getActorId(),
            command.getTraceId(),
            failedBinding.getPolicySnapshot(),
            rollbackBinding.getPolicySnapshot()
        );

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

    private void validatePartnerTemplateRequirements(ApplyTemplatePolicyCommand command) {
        if (command.getTemplateType() != TemplateType.PARTNER) {
            return;
        }
        if (!StringUtils.hasText(command.getConsumerId())) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "partner 템플릿은 consumerId가 필수입니다.");
        }

        ManagedConsumer consumer = managedConsumerRepository.findById(command.getConsumerId())
            .orElseThrow(() -> new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 consumer를 찾을 수 없습니다."));

        if (consumer.getType() != ConsumerType.PARTNER) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "partner 타입 consumer만 partner 템플릿에 연동할 수 있습니다.");
        }

        boolean hasActiveKey = !managedConsumerKeyRepository.findAllByConsumerIdAndStatus(
            command.getConsumerId(),
            ConsumerKeyStatus.ACTIVE
        ).isEmpty();
        if (!hasActiveKey) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "partner 템플릿은 ACTIVE API Key를 가진 consumer만 연동할 수 있습니다."
            );
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

    private void validateAuditContext(String actorId, String traceId) {
        // 컨트롤러 검증 누락이나 내부 호출 오류가 있어도 감사로그 필수 컨텍스트 누락을 차단한다.
        if (!StringUtils.hasText(actorId) || !StringUtils.hasText(traceId)) {
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "감사로그 필수 컨텍스트(actorId/traceId)가 누락되었습니다."
            );
        }
    }

    private void recordAudit(
        AuditAction action,
        String targetId,
        String actorId,
        String traceId,
        String beforeJson,
        String afterJson
    ) {
        auditLogWriteUseCase.record(
            action,
            AuditTargetType.TEMPLATE_POLICY,
            targetId,
            actorId,
            traceId,
            beforeJson,
            afterJson
        );
    }
}
