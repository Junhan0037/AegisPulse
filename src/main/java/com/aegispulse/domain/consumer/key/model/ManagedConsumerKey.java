package com.aegispulse.domain.consumer.key.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Consumer API Key 도메인 모델.
 * PRD 데이터 모델 ConsumerKey(id, consumerId, keyHash, status, createdAt, revokedAt)를 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ManagedConsumerKey {

    private final String id;
    private final String consumerId;
    private final String keyHash;
    private final ConsumerKeyStatus status;
    private final Instant createdAt;
    private final Instant revokedAt;

    /**
     * 신규 활성 키를 생성한다.
     */
    public static ManagedConsumerKey newActiveKey(String id, String consumerId, String keyHash) {
        return ManagedConsumerKey.builder()
            .id(id)
            .consumerId(consumerId)
            .keyHash(keyHash)
            .status(ConsumerKeyStatus.ACTIVE)
            .createdAt(Instant.now())
            .revokedAt(null)
            .build();
    }

    /**
     * 활성 키를 폐기 상태로 전환한다.
     */
    public ManagedConsumerKey revoke() {
        if (status == ConsumerKeyStatus.REVOKED) {
            return this;
        }
        return ManagedConsumerKey.builder()
            .id(id)
            .consumerId(consumerId)
            .keyHash(keyHash)
            .status(ConsumerKeyStatus.REVOKED)
            .createdAt(createdAt)
            .revokedAt(Instant.now())
            .build();
    }

    /**
     * 영속화 계층에서 도메인 객체를 복원한다.
     */
    public static ManagedConsumerKey restore(
        String id,
        String consumerId,
        String keyHash,
        ConsumerKeyStatus status,
        Instant createdAt,
        Instant revokedAt
    ) {
        return ManagedConsumerKey.builder()
            .id(id)
            .consumerId(consumerId)
            .keyHash(keyHash)
            .status(status)
            .createdAt(createdAt)
            .revokedAt(revokedAt)
            .build();
    }
}
