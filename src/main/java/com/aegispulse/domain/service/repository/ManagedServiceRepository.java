package com.aegispulse.domain.service.repository;

import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;

/**
 * Service 도메인 저장소 추상화.
 * 애플리케이션 계층이 저장 기술(JPA/외부 DB)에 의존하지 않도록 분리한다.
 */
public interface ManagedServiceRepository {

    boolean existsByEnvironmentAndName(ServiceEnvironment environment, String name);

    ManagedService save(ManagedService service);
}
