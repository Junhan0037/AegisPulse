package com.aegispulse.domain.consumer.key.repository;

import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
import java.util.List;

/**
 * ConsumerKey 도메인 저장소 추상화.
 */
public interface ManagedConsumerKeyRepository {

    /**
     * 특정 Consumer의 특정 상태 키 목록을 조회한다.
     */
    List<ManagedConsumerKey> findAllByConsumerIdAndStatus(String consumerId, ConsumerKeyStatus status);

    /**
     * ConsumerKey 도메인 모델을 저장한다.
     */
    ManagedConsumerKey save(ManagedConsumerKey consumerKey);
}
