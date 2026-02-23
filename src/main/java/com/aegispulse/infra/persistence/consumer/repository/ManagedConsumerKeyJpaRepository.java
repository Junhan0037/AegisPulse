package com.aegispulse.infra.persistence.consumer.repository;

import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.infra.persistence.consumer.entity.ManagedConsumerKeyJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ConsumerKey JPA 리포지토리.
 */
public interface ManagedConsumerKeyJpaRepository extends JpaRepository<ManagedConsumerKeyJpaEntity, String> {

    List<ManagedConsumerKeyJpaEntity> findAllByConsumerIdAndStatus(String consumerId, ConsumerKeyStatus status);
}
