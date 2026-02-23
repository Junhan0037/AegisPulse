package com.aegispulse.application.consumer.key;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.consumer.key.command.IssueConsumerKeyCommand;
import com.aegispulse.application.consumer.key.result.IssueConsumerKeyResult;
import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
import com.aegispulse.domain.consumer.key.repository.ManagedConsumerKeyRepository;
import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer API Key 발급 유스케이스 구현체.
 * FR-005의 1회 노출/해시 저장/재발급 시 기존 키 폐기 규칙을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ConsumerKeyIssueService implements IssueConsumerKeyUseCase {

    private final ManagedConsumerRepository managedConsumerRepository;
    private final ManagedConsumerKeyRepository managedConsumerKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final ApiKeyHasher apiKeyHasher;

    @Override
    @Transactional
    public IssueConsumerKeyResult issue(IssueConsumerKeyCommand command) {
        ManagedConsumer consumer = managedConsumerRepository.findById(command.getConsumerId())
            .orElseThrow(() -> new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 consumer를 찾을 수 없습니다."));

        if (consumer.getType() != ConsumerType.PARTNER) {
            throw new AegisPulseException(
                ErrorCode.INVALID_REQUEST,
                "partner 타입 consumer만 API Key를 발급할 수 있습니다."
            );
        }

        managedConsumerKeyRepository.findAllByConsumerIdAndStatus(command.getConsumerId(), ConsumerKeyStatus.ACTIVE)
            .stream()
            .map(ManagedConsumerKey::revoke)
            .forEach(managedConsumerKeyRepository::save);

        String plainKey = apiKeyGenerator.generate();
        String keyHash = apiKeyHasher.hash(plainKey);
        ManagedConsumerKey saved = managedConsumerKeyRepository.save(
            ManagedConsumerKey.newActiveKey(generateKeyId(), consumer.getId(), keyHash)
        );

        return IssueConsumerKeyResult.builder()
            .keyId(saved.getId())
            .apiKey(plainKey)
            .build();
    }

    private String generateKeyId() {
        return "key_" + UUID.randomUUID().toString().replace("-", "");
    }
}
