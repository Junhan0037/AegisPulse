package com.aegispulse.domain.consumer.repository;

import com.aegispulse.domain.consumer.model.ManagedConsumer;

/**
 * Consumer 도메인 저장소 추상화.
 */
public interface ManagedConsumerRepository {

    /**
     * Consumer 이름 중복 여부를 조회한다.
     */
    boolean existsByName(String name);

    /**
     * Consumer 도메인 모델을 저장한다.
     */
    ManagedConsumer save(ManagedConsumer consumer);
}
