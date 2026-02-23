package com.aegispulse.application.consumer.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumerKeyIssueServiceTest {

    @Mock
    private ManagedConsumerRepository managedConsumerRepository;

    @Mock
    private ManagedConsumerKeyRepository managedConsumerKeyRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private ApiKeyHasher apiKeyHasher;

    @InjectMocks
    private ConsumerKeyIssueService consumerKeyIssueService;

    @Test
    @DisplayName("consumerId가 없으면 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenConsumerDoesNotExist() {
        IssueConsumerKeyCommand command = IssueConsumerKeyCommand.builder().consumerId("csm_missing").build();
        given(managedConsumerRepository.findById("csm_missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> consumerKeyIssueService.issue(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });

        then(managedConsumerKeyRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("partner 타입이 아니면 INVALID_REQUEST 예외를 던진다")
    void shouldThrowBadRequestWhenConsumerTypeIsNotPartner() {
        IssueConsumerKeyCommand command = IssueConsumerKeyCommand.builder().consumerId("csm_internal").build();
        given(managedConsumerRepository.findById("csm_internal"))
            .willReturn(Optional.of(ManagedConsumer.newConsumer("csm_internal", "internal-client-a", ConsumerType.INTERNAL)));

        assertThatThrownBy(() -> consumerKeyIssueService.issue(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            });

        then(managedConsumerKeyRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("최초 발급이면 기존 폐기 없이 ACTIVE 키를 저장하고 원문 키를 반환한다")
    void shouldIssueActiveKeyWhenNoPreviousActiveKeyExists() {
        IssueConsumerKeyCommand command = IssueConsumerKeyCommand.builder().consumerId("csm_partner").build();

        given(managedConsumerRepository.findById("csm_partner"))
            .willReturn(Optional.of(ManagedConsumer.newConsumer("csm_partner", "partner-client-a", ConsumerType.PARTNER)));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.ACTIVE))
            .willReturn(List.of());
        given(apiKeyGenerator.generate()).willReturn("ak_plain_key_once");
        given(apiKeyHasher.hash("ak_plain_key_once")).willReturn("pbkdf2$120000$salt$hash");
        given(managedConsumerKeyRepository.save(any(ManagedConsumerKey.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        IssueConsumerKeyResult result = consumerKeyIssueService.issue(command);

        assertThat(result.getKeyId()).startsWith("key_");
        assertThat(result.getApiKey()).isEqualTo("ak_plain_key_once");

        ArgumentCaptor<ManagedConsumerKey> keyCaptor = ArgumentCaptor.forClass(ManagedConsumerKey.class);
        then(managedConsumerKeyRepository).should().save(keyCaptor.capture());
        ManagedConsumerKey saved = keyCaptor.getValue();
        assertThat(saved.getConsumerId()).isEqualTo("csm_partner");
        assertThat(saved.getStatus()).isEqualTo(ConsumerKeyStatus.ACTIVE);
        assertThat(saved.getKeyHash()).isEqualTo("pbkdf2$120000$salt$hash");
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("재발급이면 기존 ACTIVE 키를 모두 REVOKED로 저장한 뒤 새 키를 발급한다")
    void shouldRevokePreviousActiveKeysBeforeIssuingNewKey() {
        IssueConsumerKeyCommand command = IssueConsumerKeyCommand.builder().consumerId("csm_partner").build();

        ManagedConsumerKey activeOne = ManagedConsumerKey.newActiveKey("key_old_01", "csm_partner", "pbkdf2$old1");
        ManagedConsumerKey activeTwo = ManagedConsumerKey.newActiveKey("key_old_02", "csm_partner", "pbkdf2$old2");

        given(managedConsumerRepository.findById("csm_partner"))
            .willReturn(Optional.of(ManagedConsumer.newConsumer("csm_partner", "partner-client-a", ConsumerType.PARTNER)));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.ACTIVE))
            .willReturn(List.of(activeOne, activeTwo));
        given(apiKeyGenerator.generate()).willReturn("ak_reissued_once");
        given(apiKeyHasher.hash("ak_reissued_once")).willReturn("pbkdf2$new");
        given(managedConsumerKeyRepository.save(any(ManagedConsumerKey.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        IssueConsumerKeyResult result = consumerKeyIssueService.issue(command);

        assertThat(result.getApiKey()).isEqualTo("ak_reissued_once");

        ArgumentCaptor<ManagedConsumerKey> keyCaptor = ArgumentCaptor.forClass(ManagedConsumerKey.class);
        then(managedConsumerKeyRepository).should(times(3)).save(keyCaptor.capture());
        List<ManagedConsumerKey> savedKeys = keyCaptor.getAllValues();

        assertThat(savedKeys.get(0).getStatus()).isEqualTo(ConsumerKeyStatus.REVOKED);
        assertThat(savedKeys.get(0).getRevokedAt()).isNotNull();
        assertThat(savedKeys.get(1).getStatus()).isEqualTo(ConsumerKeyStatus.REVOKED);
        assertThat(savedKeys.get(1).getRevokedAt()).isNotNull();
        assertThat(savedKeys.get(2).getStatus()).isEqualTo(ConsumerKeyStatus.ACTIVE);
        assertThat(savedKeys.get(2).getRevokedAt()).isNull();
        assertThat(savedKeys.get(2).getKeyHash()).isEqualTo("pbkdf2$new");
    }
}
