package com.aegispulse.domain.service.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Service 관리 도메인 모델.
 * PRD 데이터 모델(Service)의 핵심 속성을 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ManagedService {

    private final String id;
    private final String name;
    private final String upstreamUrl;
    private final ServiceEnvironment environment;
    private final ServiceStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * 신규 Service 등록 도메인 객체를 생성한다.
     * 생성 시점의 createdAt/updatedAt을 동일 값으로 맞춘다.
     */
    public static ManagedService newService(
        String id,
        String name,
        String upstreamUrl,
        ServiceEnvironment environment
    ) {
        Instant now = Instant.now();
        return ManagedService.builder()
            .id(id)
            .name(name)
            .upstreamUrl(upstreamUrl)
            .environment(environment)
            .status(ServiceStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * 영속화 계층 복원을 위한 팩토리 메서드.
     */
    public static ManagedService restore(
        String id,
        String name,
        String upstreamUrl,
        ServiceEnvironment environment,
        ServiceStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        return ManagedService.builder()
            .id(id)
            .name(name)
            .upstreamUrl(upstreamUrl)
            .environment(environment)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
