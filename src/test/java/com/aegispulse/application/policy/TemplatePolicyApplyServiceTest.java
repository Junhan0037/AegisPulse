package com.aegispulse.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import java.util.List;
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
        then(policyBindingRepository).should(never()).save(any());
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

        then(policyBindingRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("유효한 요청이면 정책 스냅샷을 저장하고 적용 결과를 반환한다")
    void shouldSavePolicyBindingAndReturnResult() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId(null)
            .templateType(TemplateType.PARTNER)
            .build();

        TemplatePolicyProfile profile = TemplatePolicyProfile.of(
            TemplateType.PARTNER,
            TrafficPolicy.of(100, 1_000, 3_000, 3_000, 2, 100),
            AuthPolicy.of(AuthType.API_KEY_REQUIRED, true, List.of(), 1_048_576),
            true
        );

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(templatePolicyMapper.map(TemplateType.PARTNER)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willReturn("{\"templateType\":\"PARTNER\"}");
        // 저장소는 저장된 도메인 객체를 그대로 반환한다고 가정한다.
        given(policyBindingRepository.save(any(PolicyBinding.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApplyTemplatePolicyResult result = templatePolicyApplyService.apply(command);

        assertThat(result.getBindingId()).startsWith("plb_");
        assertThat(result.getBindingId()).hasSize(36);
        assertThat(result.getServiceId()).isEqualTo("svc_01");
        assertThat(result.getRouteId()).isNull();
        assertThat(result.getTemplateType()).isEqualTo("PARTNER");
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getAppliedAt()).isNotNull();

        ArgumentCaptor<PolicyBinding> bindingCaptor = ArgumentCaptor.forClass(PolicyBinding.class);
        then(policyBindingRepository).should().save(bindingCaptor.capture());
        // 유스케이스가 직렬화된 스냅샷 문자열을 바인딩에 저장하는지 검증한다.
        assertThat(bindingCaptor.getValue().getPolicySnapshot()).isEqualTo("{\"templateType\":\"PARTNER\"}");
    }

    @Test
    @DisplayName("스냅샷 직렬화 실패 시 INTERNAL_SERVER_ERROR 예외로 변환한다")
    void shouldThrowInternalServerErrorWhenSnapshotSerializationFails() throws Exception {
        ApplyTemplatePolicyCommand command = ApplyTemplatePolicyCommand.builder()
            .serviceId("svc_01")
            .routeId(null)
            .templateType(TemplateType.PUBLIC)
            .build();

        TemplatePolicyProfile profile = TemplatePolicyProfile.of(
            TemplateType.PUBLIC,
            TrafficPolicy.of(100, 1_000, 3_000, 3_000, 2, 100),
            AuthPolicy.of(AuthType.NONE, false, List.of(), null),
            true
        );

        given(managedServiceRepository.existsById("svc_01")).willReturn(true);
        given(templatePolicyMapper.map(TemplateType.PUBLIC)).willReturn(profile);
        given(objectMapper.writeValueAsString(profile)).willThrow(newJsonProcessingException("serialize failure"));

        assertThatThrownBy(() -> templatePolicyApplyService.apply(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
            });

        then(policyBindingRepository).should(never()).save(any());
    }

    private JsonProcessingException newJsonProcessingException(String message) {
        // JsonProcessingException은 추상 클래스라 테스트에서는 익명 클래스로 생성한다.
        return new JsonProcessingException(message) {
        };
    }
}
