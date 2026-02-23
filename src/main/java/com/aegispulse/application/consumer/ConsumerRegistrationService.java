package com.aegispulse.application.consumer;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.consumer.command.RegisterConsumerCommand;
import com.aegispulse.application.consumer.result.RegisterConsumerResult;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer 등록 유스케이스 구현체.
 * FR-005의 Consumer 등록과 이름 중복 방어를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ConsumerRegistrationService implements ConsumerRegistrationUseCase {

    private final ManagedConsumerRepository managedConsumerRepository;

    @Override
    @Transactional
    public RegisterConsumerResult register(RegisterConsumerCommand command) {
        if (managedConsumerRepository.existsByName(command.getName())) {
            throw duplicatedException();
        }

        ManagedConsumer candidate = ManagedConsumer.newConsumer(
            generateConsumerId(),
            command.getName(),
            command.getType()
        );

        try {
            ManagedConsumer saved = managedConsumerRepository.save(candidate);
            return RegisterConsumerResult.builder()
                .consumerId(saved.getId())
                .name(saved.getName())
                .type(saved.getType().name())
                .build();
        } catch (DataIntegrityViolationException exception) {
            // 동시성 경합 시 DB 유니크 제약 위반을 도메인 에러로 변환한다.
            throw duplicatedException();
        }
    }

    private String generateConsumerId() {
        return "csm_" + UUID.randomUUID().toString().replace("-", "");
    }

    private AegisPulseException duplicatedException() {
        return new AegisPulseException(
            ErrorCode.CONSUMER_DUPLICATED,
            "consumer name already exists"
        );
    }
}
