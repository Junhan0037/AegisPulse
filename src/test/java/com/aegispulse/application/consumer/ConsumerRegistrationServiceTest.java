package com.aegispulse.application.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.consumer.command.RegisterConsumerCommand;
import com.aegispulse.application.consumer.result.RegisterConsumerResult;
import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ConsumerRegistrationServiceTest {

    @Mock
    private ManagedConsumerRepository managedConsumerRepository;

    @InjectMocks
    private ConsumerRegistrationService consumerRegistrationService;

    @Test
    @DisplayName("name이 중복되면 CONSUMER_DUPLICATED 예외를 던진다")
    void shouldThrowDuplicatedExceptionWhenConsumerNameAlreadyExists() {
        RegisterConsumerCommand command = RegisterConsumerCommand.builder()
            .name("partner-client-a")
            .type(ConsumerType.PARTNER)
            .build();

        given(managedConsumerRepository.existsByName("partner-client-a")).willReturn(true);

        assertThatThrownBy(() -> consumerRegistrationService.register(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.CONSUMER_DUPLICATED);
            });

        then(managedConsumerRepository).should().existsByName("partner-client-a");
        then(managedConsumerRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("유효한 요청이면 consumerId를 생성해 저장하고 결과를 반환한다")
    void shouldSaveAndReturnRegisteredConsumer() {
        RegisterConsumerCommand command = RegisterConsumerCommand.builder()
            .name("internal-client-a")
            .type(ConsumerType.INTERNAL)
            .build();

        given(managedConsumerRepository.existsByName("internal-client-a")).willReturn(false);
        given(managedConsumerRepository.save(any(ManagedConsumer.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RegisterConsumerResult result = consumerRegistrationService.register(command);

        assertThat(result.getConsumerId()).startsWith("csm_");
        assertThat(result.getConsumerId()).hasSize(36);
        assertThat(result.getName()).isEqualTo("internal-client-a");
        assertThat(result.getType()).isEqualTo("INTERNAL");

        ArgumentCaptor<ManagedConsumer> consumerCaptor = ArgumentCaptor.forClass(ManagedConsumer.class);
        then(managedConsumerRepository).should().save(consumerCaptor.capture());
        assertThat(consumerCaptor.getValue().getType()).isEqualTo(ConsumerType.INTERNAL);
        assertThat(consumerCaptor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("동시성 경합으로 유니크 제약이 깨지면 CONSUMER_DUPLICATED 예외로 변환한다")
    void shouldTranslateDataIntegrityViolationToDuplicatedException() {
        RegisterConsumerCommand command = RegisterConsumerCommand.builder()
            .name("partner-client-a")
            .type(ConsumerType.PARTNER)
            .build();

        given(managedConsumerRepository.existsByName("partner-client-a")).willReturn(false);
        given(managedConsumerRepository.save(any(ManagedConsumer.class)))
            .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        assertThatThrownBy(() -> consumerRegistrationService.register(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.CONSUMER_DUPLICATED);
            });
    }
}
