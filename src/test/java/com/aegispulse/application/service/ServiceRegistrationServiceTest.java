package com.aegispulse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.service.command.RegisterServiceCommand;
import com.aegispulse.application.service.result.RegisterServiceResult;
import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ServiceRegistrationServiceTest {

    @Mock
    private ManagedServiceRepository managedServiceRepository;

    @InjectMocks
    private ServiceRegistrationService serviceRegistrationService;

    @Test
    @DisplayName("동일 환경에서 name이 중복되면 SERVICE_DUPLICATED 예외를 던진다")
    void shouldThrowDuplicatedExceptionWhenServiceNameAlreadyExists() {
        RegisterServiceCommand command = RegisterServiceCommand.builder()
            .name("partner-payment-api")
            .upstreamUrl("https://payment.internal.svc")
            .environment(ServiceEnvironment.PROD)
            .build();

        given(managedServiceRepository.existsByEnvironmentAndName(ServiceEnvironment.PROD, "partner-payment-api"))
            .willReturn(true);

        assertThatThrownBy(() -> serviceRegistrationService.register(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.SERVICE_DUPLICATED);
            });

        then(managedServiceRepository).should().existsByEnvironmentAndName(ServiceEnvironment.PROD, "partner-payment-api");
        then(managedServiceRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("유효한 등록 요청이면 serviceId를 생성해 저장하고 결과를 반환한다")
    void shouldSaveAndReturnRegisteredService() {
        RegisterServiceCommand command = RegisterServiceCommand.builder()
            .name("partner-payment-api")
            .upstreamUrl("https://payment.internal.svc")
            .environment(ServiceEnvironment.STAGE)
            .build();

        given(managedServiceRepository.existsByEnvironmentAndName(ServiceEnvironment.STAGE, "partner-payment-api"))
            .willReturn(false);
        // 저장소는 저장된 도메인 객체를 그대로 반환하도록 가정한다.
        given(managedServiceRepository.save(any(ManagedService.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RegisterServiceResult result = serviceRegistrationService.register(command);

        assertThat(result.getServiceId()).startsWith("svc_");
        assertThat(result.getServiceId()).hasSize(36);
        assertThat(result.getName()).isEqualTo("partner-payment-api");
        assertThat(result.getEnvironment()).isEqualTo("STAGE");

        ArgumentCaptor<ManagedService> serviceCaptor = ArgumentCaptor.forClass(ManagedService.class);
        then(managedServiceRepository).should().save(serviceCaptor.capture());
        assertThat(serviceCaptor.getValue().getName()).isEqualTo("partner-payment-api");
        assertThat(serviceCaptor.getValue().getEnvironment()).isEqualTo(ServiceEnvironment.STAGE);
    }

    @Test
    @DisplayName("동시성 경합으로 유니크 제약이 깨지면 SERVICE_DUPLICATED 예외로 변환한다")
    void shouldTranslateDataIntegrityViolationToDuplicatedException() {
        RegisterServiceCommand command = RegisterServiceCommand.builder()
            .name("partner-payment-api")
            .upstreamUrl("https://payment.internal.svc")
            .environment(ServiceEnvironment.PROD)
            .build();

        given(managedServiceRepository.existsByEnvironmentAndName(ServiceEnvironment.PROD, "partner-payment-api"))
            .willReturn(false);
        given(managedServiceRepository.save(any(ManagedService.class)))
            .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        assertThatThrownBy(() -> serviceRegistrationService.register(command))
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.SERVICE_DUPLICATED);
            });
    }
}
