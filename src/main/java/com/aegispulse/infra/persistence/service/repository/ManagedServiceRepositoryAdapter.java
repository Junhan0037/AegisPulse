package com.aegispulse.infra.persistence.service.repository;

import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.aegispulse.infra.persistence.service.entity.ManagedServiceJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도메인 저장소 인터페이스를 JPA 구현체로 연결하는 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class ManagedServiceRepositoryAdapter implements ManagedServiceRepository {

    private final ManagedServiceJpaRepository managedServiceJpaRepository;

    @Override
    public boolean existsById(String serviceId) {
        return managedServiceJpaRepository.existsById(serviceId);
    }

    @Override
    public boolean existsByEnvironmentAndName(ServiceEnvironment environment, String name) {
        return managedServiceJpaRepository.existsByEnvironmentAndName(environment, name);
    }

    @Override
    public ManagedService save(ManagedService service) {
        return managedServiceJpaRepository.save(ManagedServiceJpaEntity.fromDomain(service)).toDomain();
    }
}
