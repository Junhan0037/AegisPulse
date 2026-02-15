package com.aegispulse.domain.service.repository;

import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;

/**
 * Service 도메인 저장소 추상화.
 * 애플리케이션 계층이 저장 기술(JPA/외부 DB)에 의존하지 않도록 분리한다.
 */
public interface ManagedServiceRepository {

    /**
     * serviceId 기준 존재 여부를 조회한다.
     */
    boolean existsById(String serviceId);

    /**
     * 동일 환경의 서비스 이름 중복 여부를 조회한다.
     */
    boolean existsByEnvironmentAndName(ServiceEnvironment environment, String name);

    /**
     * 서비스 도메인 모델을 저장한다.
     */
    ManagedService save(ManagedService service);
}
