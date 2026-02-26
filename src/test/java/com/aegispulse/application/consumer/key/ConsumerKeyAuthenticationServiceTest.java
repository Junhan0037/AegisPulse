package com.aegispulse.application.consumer.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumerKeyAuthenticationServiceTest {

    @Mock
    private PolicyBindingRepository policyBindingRepository;

    @Mock
    private ManagedConsumerRepository managedConsumerRepository;

    @Mock
    private ManagedConsumerKeyRepository managedConsumerKeyRepository;

    @Mock
    private ApiKeyHasher apiKeyHasher;

    @InjectMocks
    private ConsumerKeyAuthenticationService consumerKeyAuthenticationService;

    @Test
    @DisplayName("partner 템플릿 + ACTIVE 키가 일치하면 인증 성공을 반환한다")
    void shouldAuthenticateWhenActiveKeyMatches() {
        AuthenticateConsumerKeyCommand command = validCommand("ak_active_key");
        ManagedConsumerKey activeKey = activeKey("key_active_01", "hash_active_01");

        given(policyBindingRepository.findLatest("svc_01", "rte_01"))
            .willReturn(Optional.of(partnerBinding("svc_01", "rte_01")));
        given(managedConsumerRepository.findById("csm_partner"))
            .willReturn(Optional.of(partnerConsumer("csm_partner")));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.ACTIVE))
            .willReturn(List.of(activeKey));
        given(apiKeyHasher.matches("ak_active_key", "hash_active_01")).willReturn(true);

        AuthenticateConsumerKeyResult result = consumerKeyAuthenticationService.authenticate(command);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getConsumerId()).isEqualTo("csm_partner");
        assertThat(result.getKeyId()).isEqualTo("key_active_01");
        then(managedConsumerKeyRepository).should(never()).findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.REVOKED);
    }

    @Test
    @DisplayName("partner 템플릿이 아니면 FORBIDDEN 예외를 던진다")
    void shouldThrowForbiddenWhenPartnerTemplateIsNotApplied() {
        AuthenticateConsumerKeyCommand command = validCommand("ak_active_key");

        given(policyBindingRepository.findLatest("svc_01", "rte_01"))
            .willReturn(Optional.of(nonPartnerBinding("svc_01", "rte_01")));

        assertThatThrownBy(() -> consumerKeyAuthenticationService.authenticate(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            });
    }

    @Test
    @DisplayName("consumer가 없으면 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenConsumerDoesNotExist() {
        AuthenticateConsumerKeyCommand command = validCommand("ak_active_key");

        given(policyBindingRepository.findLatest("svc_01", "rte_01"))
            .willReturn(Optional.of(partnerBinding("svc_01", "rte_01")));
        given(managedConsumerRepository.findById("csm_partner")).willReturn(Optional.empty());

        assertThatThrownBy(() -> consumerKeyAuthenticationService.authenticate(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });
    }

    @Test
    @DisplayName("폐기된 키가 일치하면 FORBIDDEN 예외를 던진다")
    void shouldThrowForbiddenWhenRevokedKeyMatches() {
        AuthenticateConsumerKeyCommand command = validCommand("ak_revoked_key");
        ManagedConsumerKey activeKey = activeKey("key_active_01", "hash_active_01");
        ManagedConsumerKey revokedKey = revokedKey("key_revoked_01", "hash_revoked_01");

        given(policyBindingRepository.findLatest("svc_01", "rte_01"))
            .willReturn(Optional.of(partnerBinding("svc_01", "rte_01")));
        given(managedConsumerRepository.findById("csm_partner"))
            .willReturn(Optional.of(partnerConsumer("csm_partner")));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.ACTIVE))
            .willReturn(List.of(activeKey));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.REVOKED))
            .willReturn(List.of(revokedKey));
        given(apiKeyHasher.matches("ak_revoked_key", "hash_active_01")).willReturn(false);
        given(apiKeyHasher.matches("ak_revoked_key", "hash_revoked_01")).willReturn(true);

        assertThatThrownBy(() -> consumerKeyAuthenticationService.authenticate(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            });
    }

    @Test
    @DisplayName("ACTIVE/REVOKED 어디에도 매칭되지 않으면 UNAUTHORIZED 예외를 던진다")
    void shouldThrowUnauthorizedWhenKeyDoesNotMatchAnyStoredKey() {
        AuthenticateConsumerKeyCommand command = validCommand("ak_unknown_key");
        ManagedConsumerKey activeKey = activeKey("key_active_01", "hash_active_01");
        ManagedConsumerKey revokedKey = revokedKey("key_revoked_01", "hash_revoked_01");

        given(policyBindingRepository.findLatest("svc_01", "rte_01"))
            .willReturn(Optional.of(partnerBinding("svc_01", "rte_01")));
        given(managedConsumerRepository.findById("csm_partner"))
            .willReturn(Optional.of(partnerConsumer("csm_partner")));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.ACTIVE))
            .willReturn(List.of(activeKey));
        given(managedConsumerKeyRepository.findAllByConsumerIdAndStatus("csm_partner", ConsumerKeyStatus.REVOKED))
            .willReturn(List.of(revokedKey));
        given(apiKeyHasher.matches("ak_unknown_key", "hash_active_01")).willReturn(false);
        given(apiKeyHasher.matches("ak_unknown_key", "hash_revoked_01")).willReturn(false);

        assertThatThrownBy(() -> consumerKeyAuthenticationService.authenticate(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            });
    }

    @Test
    @DisplayName("X-API-Key가 비어 있으면 UNAUTHORIZED 예외를 던진다")
    void shouldThrowUnauthorizedWhenApiKeyIsBlank() {
        AuthenticateConsumerKeyCommand command = validCommand("   ");

        given(policyBindingRepository.findLatest("svc_01", "rte_01"))
            .willReturn(Optional.of(partnerBinding("svc_01", "rte_01")));
        given(managedConsumerRepository.findById("csm_partner"))
            .willReturn(Optional.of(partnerConsumer("csm_partner")));

        assertThatThrownBy(() -> consumerKeyAuthenticationService.authenticate(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            });
    }

    private AuthenticateConsumerKeyCommand validCommand(String apiKey) {
        return AuthenticateConsumerKeyCommand.builder()
            .serviceId("svc_01")
            .routeId("rte_01")
            .consumerId("csm_partner")
            .apiKey(apiKey)
            .build();
    }

    private ManagedConsumer partnerConsumer(String consumerId) {
        return ManagedConsumer.newConsumer(consumerId, "partner-client-a", ConsumerType.PARTNER);
    }

    private ManagedConsumerKey activeKey(String keyId, String keyHash) {
        return ManagedConsumerKey.restore(
            keyId,
            "csm_partner",
            keyHash,
            ConsumerKeyStatus.ACTIVE,
            Instant.parse("2026-02-20T12:00:00Z"),
            null
        );
    }

    private ManagedConsumerKey revokedKey(String keyId, String keyHash) {
        return ManagedConsumerKey.restore(
            keyId,
            "csm_partner",
            keyHash,
            ConsumerKeyStatus.REVOKED,
            Instant.parse("2026-02-20T12:00:00Z"),
            Instant.parse("2026-02-21T12:00:00Z")
        );
    }

    private PolicyBinding partnerBinding(String serviceId, String routeId) {
        return PolicyBinding.restore(
            "plb_01",
            serviceId,
            routeId,
            TemplateType.PARTNER,
            "{\"templateType\":\"PARTNER\"}",
            1,
            Instant.parse("2026-02-20T12:00:00Z")
        );
    }

    private PolicyBinding nonPartnerBinding(String serviceId, String routeId) {
        return PolicyBinding.restore(
            "plb_02",
            serviceId,
            routeId,
            TemplateType.PUBLIC,
            "{\"templateType\":\"PUBLIC\"}",
            1,
            Instant.parse("2026-02-20T12:00:00Z")
        );
    }
}
