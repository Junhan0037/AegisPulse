package com.aegispulse.domain.consumer.repository;

import com.aegispulse.domain.consumer.model.ManagedConsumer;
import java.util.Optional;

/**
 * Consumer 도메인 저장소 추상화.
 */
public interface ManagedConsumerRepository {

    /**
     * consumerId로 Consumer를 조회한다.
     */
    Optional<ManagedConsumer> findById(String consumerId);

    /**
     * Consumer 이름 중복 여부를 조회한다.
     */
    boolean existsByName(String name);

    /**
     * Consumer 도메인 모델을 저장한다.
     */
    ManagedConsumer save(ManagedConsumer consumer);
}
