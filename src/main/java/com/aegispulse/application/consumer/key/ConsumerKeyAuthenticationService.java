package com.aegispulse.application.consumer.key;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.consumer.key.command.AuthenticateConsumerKeyCommand;
import com.aegispulse.application.consumer.key.result.AuthenticateConsumerKeyResult;
import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
import com.aegispulse.domain.consumer.key.repository.ManagedConsumerKeyRepository;
import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.domain.policy.repository.PolicyBindingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Consumer API Key 인증 유스케이스 구현체.
 * partner 템플릿 연동 상태를 확인한 뒤 키 해시 매칭으로 인증 성공/실패를 판정한다.
 */
@Service
@RequiredArgsConstructor
public class ConsumerKeyAuthenticationService implements AuthenticateConsumerKeyUseCase {

    private final PolicyBindingRepository policyBindingRepository;
    private final ManagedConsumerRepository managedConsumerRepository;
    private final ManagedConsumerKeyRepository managedConsumerKeyRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Override
    @Transactional(readOnly = true)
    public AuthenticateConsumerKeyResult authenticate(AuthenticateConsumerKeyCommand command) {
        validatePartnerTemplateBinding(command.getServiceId(), command.getRouteId());
        ManagedConsumer consumer = loadPartnerConsumer(command.getConsumerId());

        if (!StringUtils.hasText(command.getApiKey())) {
            throw new AegisPulseException(ErrorCode.UNAUTHORIZED, "X-API-Key 헤더가 필요합니다.");
        }

        List<ManagedConsumerKey> activeKeys = managedConsumerKeyRepository.findAllByConsumerIdAndStatus(
            consumer.getId(),
            ConsumerKeyStatus.ACTIVE
        );
        for (ManagedConsumerKey activeKey : activeKeys) {
            if (apiKeyHasher.matches(command.getApiKey(), activeKey.getKeyHash())) {
                return AuthenticateConsumerKeyResult.builder()
                    .authenticated(true)
                    .consumerId(consumer.getId())
                    .keyId(activeKey.getId())
                    .build();
            }
        }

        boolean matchedRevokedKey = managedConsumerKeyRepository.findAllByConsumerIdAndStatus(
            consumer.getId(),
            ConsumerKeyStatus.REVOKED
        ).stream().anyMatch(revokedKey -> apiKeyHasher.matches(command.getApiKey(), revokedKey.getKeyHash()));
        if (matchedRevokedKey) {
            throw new AegisPulseException(ErrorCode.FORBIDDEN, "폐기된 API Key입니다.");
        }

        throw new AegisPulseException(ErrorCode.UNAUTHORIZED, "API Key 인증에 실패했습니다.");
    }

    private void validatePartnerTemplateBinding(String serviceId, String routeId) {
        PolicyBinding binding = policyBindingRepository.findLatest(serviceId, routeId)
            .orElseThrow(() -> new AegisPulseException(ErrorCode.FORBIDDEN, "partner 템플릿이 적용된 정책이 없습니다."));

        if (binding.getTemplateType() != TemplateType.PARTNER) {
            throw new AegisPulseException(ErrorCode.FORBIDDEN, "partner 템플릿이 적용된 정책이 없습니다.");
        }
    }

    private ManagedConsumer loadPartnerConsumer(String consumerId) {
        ManagedConsumer consumer = managedConsumerRepository.findById(consumerId)
            .orElseThrow(() -> new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 consumer를 찾을 수 없습니다."));

        if (consumer.getType() != ConsumerType.PARTNER) {
            throw new AegisPulseException(ErrorCode.FORBIDDEN, "partner 타입 consumer만 API Key 인증을 수행할 수 있습니다.");
        }
        return consumer;
    }
}
