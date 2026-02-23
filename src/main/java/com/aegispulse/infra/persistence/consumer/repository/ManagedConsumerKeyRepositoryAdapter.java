package com.aegispulse.infra.persistence.consumer.repository;

import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
import com.aegispulse.domain.consumer.key.repository.ManagedConsumerKeyRepository;
import com.aegispulse.infra.persistence.consumer.entity.ManagedConsumerKeyJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * ConsumerKey 도메인 저장소 인터페이스를 JPA 구현체로 연결하는 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class ManagedConsumerKeyRepositoryAdapter implements ManagedConsumerKeyRepository {

    private final ManagedConsumerKeyJpaRepository managedConsumerKeyJpaRepository;

    @Override
    public List<ManagedConsumerKey> findAllByConsumerIdAndStatus(String consumerId, ConsumerKeyStatus status) {
        return managedConsumerKeyJpaRepository.findAllByConsumerIdAndStatus(consumerId, status)
            .stream()
            .map(ManagedConsumerKeyJpaEntity::toDomain)
            .toList();
    }

    @Override
    public ManagedConsumerKey save(ManagedConsumerKey consumerKey) {
        return managedConsumerKeyJpaRepository.save(ManagedConsumerKeyJpaEntity.fromDomain(consumerKey)).toDomain();
    }
}
