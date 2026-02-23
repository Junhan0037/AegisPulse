package com.aegispulse.domain.consumer.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Consumer 관리 도메인 모델.
 * PRD 데이터 모델 Consumer(id, name, type, createdAt)을 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ManagedConsumer {

    private final String id;
    private final String name;
    private final ConsumerType type;
    private final Instant createdAt;

    /**
     * 신규 Consumer 생성을 위한 팩토리 메서드.
     */
    public static ManagedConsumer newConsumer(String id, String name, ConsumerType type) {
        return ManagedConsumer.builder()
            .id(id)
            .name(name)
            .type(type)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * 영속화 계층에서 도메인 객체를 복원한다.
     */
    public static ManagedConsumer restore(String id, String name, ConsumerType type, Instant createdAt) {
        return ManagedConsumer.builder()
            .id(id)
            .name(name)
            .type(type)
            .createdAt(createdAt)
            .build();
    }
}
