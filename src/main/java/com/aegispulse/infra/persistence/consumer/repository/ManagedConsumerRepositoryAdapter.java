package com.aegispulse.infra.persistence.consumer.repository;

import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import com.aegispulse.infra.persistence.consumer.entity.ManagedConsumerJpaEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Consumer 도메인 저장소 인터페이스를 JPA 구현체로 연결하는 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class ManagedConsumerRepositoryAdapter implements ManagedConsumerRepository {

    private final ManagedConsumerJpaRepository managedConsumerJpaRepository;

    @Override
    public Optional<ManagedConsumer> findById(String consumerId) {
        return managedConsumerJpaRepository.findById(consumerId).map(ManagedConsumerJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return managedConsumerJpaRepository.existsByName(name);
    }

    @Override
    public ManagedConsumer save(ManagedConsumer consumer) {
        return managedConsumerJpaRepository.save(ManagedConsumerJpaEntity.fromDomain(consumer)).toDomain();
    }
}
