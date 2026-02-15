package com.aegispulse.application.service;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.service.command.RegisterServiceCommand;
import com.aegispulse.application.service.result.RegisterServiceResult;
import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service 등록 유스케이스 구현체.
 * FR-001의 중복 검증(동일 환경 name 충돌)과 등록 처리를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ServiceRegistrationService implements ServiceRegistrationUseCase {

    private final ManagedServiceRepository managedServiceRepository;

    @Override
    @Transactional
    public RegisterServiceResult register(RegisterServiceCommand command) {
        // 선조회로 도메인 규칙을 빠르게 검증하고, 동시성 경합은 DB 유니크 제약으로 한 번 더 방어한다.
        if (managedServiceRepository.existsByEnvironmentAndName(command.getEnvironment(), command.getName())) {
            throw duplicatedException();
        }

        ManagedService candidate = ManagedService.newService(
            generateServiceId(),
            command.getName(),
            command.getUpstreamUrl(),
            command.getEnvironment()
        );

        try {
            ManagedService saved = managedServiceRepository.save(candidate);
            return RegisterServiceResult.builder()
                .serviceId(saved.getId())
                .name(saved.getName())
                .environment(saved.getEnvironment().name())
                .build();
        } catch (DataIntegrityViolationException exception) {
            // 동시에 동일 이름 등록이 들어온 경우 DB 제약 위반을 비즈니스 에러로 변환한다.
            throw duplicatedException();
        }
    }

    private String generateServiceId() {
        return "svc_" + UUID.randomUUID().toString().replace("-", "");
    }

    private AegisPulseException duplicatedException() {
        return new AegisPulseException(
            ErrorCode.SERVICE_DUPLICATED,
            "service name already exists in this environment"
        );
    }
}
