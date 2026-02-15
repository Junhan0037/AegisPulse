package com.aegispulse.domain.route.model;

import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Route 관리 도메인 모델.
 * PRD FR-002의 핵심 필드(path/host/method/stripPath)를 불변 모델로 표현한다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ManagedRoute {

    private final String id;
    private final String serviceId;
    private final List<String> paths;
    private final List<String> hosts;
    private final List<RouteHttpMethod> methods;
    private final boolean stripPath;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * 신규 Route 등록용 도메인 객체를 생성한다.
     * createdAt/updatedAt은 동일 시각으로 초기화한다.
     */
    public static ManagedRoute newRoute(
        String id,
        String serviceId,
        List<String> paths,
        List<String> hosts,
        List<RouteHttpMethod> methods,
        boolean stripPath
    ) {
        Instant now = Instant.now();
        return ManagedRoute.builder()
            .id(id)
            .serviceId(serviceId)
            .paths(List.copyOf(paths))
            .hosts(List.copyOf(hosts))
            .methods(List.copyOf(methods))
            .stripPath(stripPath)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * 영속화 계층에서 복원할 때 사용하는 팩토리 메서드.
     */
    public static ManagedRoute restore(
        String id,
        String serviceId,
        List<String> paths,
        List<String> hosts,
        List<RouteHttpMethod> methods,
        boolean stripPath,
        Instant createdAt,
        Instant updatedAt
    ) {
        return ManagedRoute.builder()
            .id(id)
            .serviceId(serviceId)
            .paths(List.copyOf(paths))
            .hosts(List.copyOf(hosts))
            .methods(List.copyOf(methods))
            .stripPath(stripPath)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
