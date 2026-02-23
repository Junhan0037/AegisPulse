package com.aegispulse.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.policy.command.ApplyTemplatePolicyCommand;
import com.aegispulse.application.policy.result.ApplyTemplatePolicyResult;
import com.aegispulse.domain.policy.model.AuthPolicy;
import com.aegispulse.domain.policy.model.AuthType;
import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplatePolicyProfile;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.domain.policy.model.TrafficPolicy;
import com.aegispulse.domain.policy.repository.PolicyBindingRepository;
import com.aegispulse.domain.route.repository.ManagedRouteRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class TemplatePolicyApplyServiceTest {

    @Mock
    private ManagedServiceRepository managedServiceRepository;

    @Mock
    private ManagedRouteRepository managedRouteRepository;

    @Mock
    private TemplatePolicyMapper templatePolicyMapper;

    @Mock
    private PolicyBindingRepository policyBindingRepository;

    @Mock
    private PolicyDeploymentPort policyDeploymentPort;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TemplatePolicyApplyService templatePolicyApplyService;

    @Test
    @DisplayName("serviceId가 존재하지 않으면 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenServiceDoesNotExist() {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_missing")
            .routeId(null)
            .templateType(TemplateType.PUBLIC)
            .build();

        given(managedServiceRepository.existsById("svc_missing")).willReturn(false);

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });

        then(managedRouteRepository).should(never()).existsByIdAndServiceId(any(), any());
        then(policyBindingRepository).should(never()).findLatest(any(), any());
        then(policyBindingRepository).should(never()).save(any());
        then(policyDeploymentPort).should(never()).apply(any());
    }

    @Test
    @DisplayName("routeId가 지정됐지만 서비스 소속이 아니면 RESOURCE_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenRouteDoesNotBelongToService() {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId("rte_missing")
            .templateType(TemplateType.INTERNAL)
            .build();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(managedRouteRepository.existsByIdAndServiceId("rte_missing", "svc_01")).willReturn(false);

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            });

        then(policyBindingRepository).should(never()).findLatest(any(), any());
        then(policyBindingRepository).should(never()).save(any());
        then(policyDeploymentPort).should(never()).apply(any());
    }

    @Test
    @DisplayName("이전 스냅샷이 없고 적용이 성공하면 version 1로 저장하고 결과를 반환한다")
    void shouldSavePolicyBindingAndReturnResultWhenApplySucceeds() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId(null)
            .templateType(TemplateType.PARTNER)
            .build();

        TemplatePolicyProfile profile = partnerProfile();
        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(policyBindingRepository.findLatest("svc_01", null)).willReturn(Optional.empty());
        given(templatePolicyMapper.map(TemplateType.PARTNER)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willReturn("{\"templateType\":\"PARTNER\"}");
        given(policyBindingRepository.save(any(PolicyBinding.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApplyTemplatePolicyResult result = templatePolicyApplyService.apply(command);

        assertThat(result.getBindingId()).startsWith("plb_");
        assertThat(result.getBindingId()).hasSize(36);
        assertThat(result.getServiceId()).isEqualTo("svc_01");
        assertThat(result.getRouteId()).isNull();
        assertThat(result.getTemplateType()).isEqualTo("PARTNER");
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getAppliedAt()).isNotNull();

        ArgumentCaptor<PolicyBinding> saveCaptor = ArgumentCaptor.forClass(PolicyBinding.class);
        then(policyBindingRepository).should().save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getPolicySnapshot()).isEqualTo("{\"templateType\":\"PARTNER\"}");
        assertThat(saveCaptor.getValue().getVersion()).isEqualTo(1);

        then(policyDeploymentPort).should().apply(any(PolicyBinding.class));
    }

    @Test
    @DisplayName("이전 스냅샷이 있으면 다음 버전으로 저장한다")
    void shouldIncreaseVersionFromLatestSnapshot() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId("rte_01")
            .templateType(TemplateType.PUBLIC)
            .build();

        PolicyBinding previous = restoredBinding(
            "plb_prev",
            "svc_01",
            "rte_01",
            TemplateType.INTERNAL,
            "{\"templateType\":\"INTERNAL\"}",
            7
        );
        TemplatePolicyProfile profile = publicProfile();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(managedRouteRepository.existsByIdAndServiceId("rte_01", "svc_01")).willReturn(true);
        given(policyBindingRepository.findLatest("svc_01", "rte_01")).willReturn(Optional.of(previous));
        given(templatePolicyMapper.map(TemplateType.PUBLIC)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willReturn("{\"templateType\":\"PUBLIC\"}");
        given(policyBindingRepository.save(any(PolicyBinding.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApplyTemplatePolicyResult result = templatePolicyApplyService.apply(command);

        assertThat(result.getVersion()).isEqualTo(8);

        ArgumentCaptor<PolicyBinding> saveCaptor = ArgumentCaptor.forClass(PolicyBinding.class);
        then(policyBindingRepository).should().save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getVersion()).isEqualTo(8);
        then(policyDeploymentPort).should().apply(any(PolicyBinding.class));
    }

    @Test
    @DisplayName("적용 실패 시 이전 스냅샷이 있으면 자동 롤백한다")
    void shouldRollbackWhenApplyFailsAndPreviousSnapshotExists() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId("rte_01")
            .templateType(TemplateType.PARTNER)
            .build();

        PolicyBinding previous = restoredBinding(
            "plb_prev",
            "svc_01",
            "rte_01",
            TemplateType.PUBLIC,
            "{\"templateType\":\"PUBLIC\"}",
            3
        );
        TemplatePolicyProfile profile = partnerProfile();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(managedRouteRepository.existsByIdAndServiceId("rte_01", "svc_01")).willReturn(true);
        given(policyBindingRepository.findLatest("svc_01", "rte_01")).willReturn(Optional.of(previous));
        given(templatePolicyMapper.map(TemplateType.PARTNER)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willReturn("{\"templateType\":\"PARTNER\"}");
        given(policyBindingRepository.save(any(PolicyBinding.class))).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("apply failed"))
            .doNothing()
            .when(policyDeploymentPort)
            .apply(any(PolicyBinding.class));

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                assertThat(aegisPulseException.getMessage()).contains("자동 롤백");
            });

        ArgumentCaptor<PolicyBinding> saveCaptor = ArgumentCaptor.forClass(PolicyBinding.class);
        then(policyBindingRepository).should(times(2)).save(saveCaptor.capture());
        List<PolicyBinding> savedBindings = saveCaptor.getAllValues();

        PolicyBinding appliedBinding = savedBindings.getFirst();
        PolicyBinding rollbackBinding = savedBindings.get(1);

        assertThat(appliedBinding.getVersion()).isEqualTo(4);
        assertThat(appliedBinding.getTemplateType()).isEqualTo(TemplateType.PARTNER);
        assertThat(rollbackBinding.getVersion()).isEqualTo(5);
        assertThat(rollbackBinding.getTemplateType()).isEqualTo(TemplateType.PUBLIC);
        assertThat(rollbackBinding.getPolicySnapshot()).isEqualTo(previous.getPolicySnapshot());

        then(policyDeploymentPort).should(times(2)).apply(any(PolicyBinding.class));
    }

    @Test
    @DisplayName("최초 적용 실패 시 이전 스냅샷이 없으면 실패 이력만 저장한다")
    void shouldKeepFailedSnapshotWhenApplyFailsWithoutPreviousSnapshot() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId(null)
            .templateType(TemplateType.PUBLIC)
            .build();

        TemplatePolicyProfile profile = publicProfile();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(policyBindingRepository.findLatest("svc_01", null)).willReturn(Optional.empty());
        given(templatePolicyMapper.map(TemplateType.PUBLIC)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willReturn("{\"templateType\":\"PUBLIC\"}");
        given(policyBindingRepository.save(any(PolicyBinding.class))).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("apply failed"))
            .when(policyDeploymentPort)
            .apply(any(PolicyBinding.class));

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                assertThat(aegisPulseException.getMessage()).contains("이전 스냅샷이 없습니다");
            });

        then(policyBindingRepository).should(times(1)).save(any(PolicyBinding.class));
        then(policyDeploymentPort).should(times(1)).apply(any(PolicyBinding.class));
    }

    @Test
    @DisplayName("자동 롤백 적용까지 실패하면 INTERNAL_SERVER_ERROR를 반환한다")
    void shouldThrowInternalServerErrorWhenRollbackAlsoFails() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId("rte_01")
            .templateType(TemplateType.PUBLIC)
            .build();

        PolicyBinding previous = restoredBinding(
            "plb_prev",
            "svc_01",
            "rte_01",
            TemplateType.INTERNAL,
            "{\"templateType\":\"INTERNAL\"}",
            2
        );
        TemplatePolicyProfile profile = publicProfile();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(managedRouteRepository.existsByIdAndServiceId("rte_01", "svc_01")).willReturn(true);
        given(policyBindingRepository.findLatest("svc_01", "rte_01")).willReturn(Optional.of(previous));
        given(templatePolicyMapper.map(TemplateType.PUBLIC)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willReturn("{\"templateType\":\"PUBLIC\"}");
        given(policyBindingRepository.save(any(PolicyBinding.class))).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("apply failed"))
            .doThrow(new RuntimeException("rollback failed"))
            .when(policyDeploymentPort)
            .apply(any(PolicyBinding.class));

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                assertThat(aegisPulseException.getMessage()).contains("자동 롤백에 실패");
            });

        then(policyBindingRepository).should(times(2)).save(any(PolicyBinding.class));
        then(policyDeploymentPort).should(times(2)).apply(any(PolicyBinding.class));
    }

    @Test
    @DisplayName("스냅샷 직렬화 실패 시 INTERNAL_SERVER_ERROR 예외로 변환한다")
    void shouldThrowInternalServerErrorWhenSnapshotSerializationFails() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId(null)
            .templateType(TemplateType.PUBLIC)
            .build();

        TemplatePolicyProfile profile = publicProfile();

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(policyBindingRepository.findLatest("svc_01", null)).willReturn(Optional.empty());
        given(templatePolicyMapper.map(TemplateType.PUBLIC)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willThrow(newJsonProcessingException("serialize failure"));

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
            });

        then(policyBindingRepository).should(never()).save(any());
        then(policyDeploymentPort).should(never()).apply(any());
    }

    private TemplatePolicyProfile publicProfile() {
        return TemplatePolicyProfile.of(
            TemplateType.PUBLIC,
            TrafficPolicy.of(100, 1_000, 3_000, 3_000, 2, 100),
            AuthPolicy.of(AuthType.NONE, false, List.of(), null),
            true
        );
    }

    private TemplatePolicyProfile partnerProfile() {
        return TemplatePolicyProfile.of(
            TemplateType.PARTNER,
            TrafficPolicy.of(100, 1_000, 3_000, 3_000, 2, 100),
            AuthPolicy.of(AuthType.API_KEY_REQUIRED, true, List.of(), 1_048_576),
            true
        );
    }

    private PolicyBinding restoredBinding(
        String id,
        String serviceId,
        String routeId,
        TemplateType templateType,
        String policySnapshot,
        int version
    ) {
        return PolicyBinding.restore(
            id,
            serviceId,
            routeId,
            templateType,
            policySnapshot,
            version,
            Instant.parse("2026-02-20T12:00:00Z")
        );
    }

    private JsonProcessingException newJsonProcessingException(String message) {
        // JsonProcessingException은 추상 클래스라 테스트에서는 익명 클래스로 생성한다.
        return new JsonProcessingException(message) {
        };
    }
}
