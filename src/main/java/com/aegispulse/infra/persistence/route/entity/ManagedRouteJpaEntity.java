package com.aegispulse.infra.persistence.route.entity;

import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.model.RouteHttpMethod;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Route 영속화 JPA 엔티티.
 * PRD 권장 인덱스(Route(serviceId))를 반영한다.
 */
@Entity
@Table(
    name = "managed_routes",
    indexes = {
        @Index(name = "idx_managed_routes_service_id", columnList = "service_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedRouteJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(name = "service_id", nullable = false, length = 40)
    private String serviceId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "managed_route_paths", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "path_value", nullable = false, length = 1024)
    private List<String> paths = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "managed_route_hosts", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "host_value", nullable = false, length = 255)
    private List<String> hosts = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "managed_route_methods", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "method_value", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private List<RouteHttpMethod> methods = new ArrayList<>();

    @Column(name = "strip_path", nullable = false)
    private boolean stripPath;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * 생성 시각 기본값과 수정 시각을 보정한다.
     */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    /**
     * 수정 시 updatedAt을 현재 시각으로 갱신한다.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static ManagedRouteJpaEntity fromDomain(ManagedRoute route) {
        ManagedRouteJpaEntity entity = new ManagedRouteJpaEntity();
        entity.setId(route.getId());
        entity.setServiceId(route.getServiceId());
        entity.setPaths(new ArrayList<>(route.getPaths()));
        entity.setHosts(new ArrayList<>(route.getHosts()));
        entity.setMethods(new ArrayList<>(route.getMethods()));
        entity.setStripPath(route.isStripPath());
        entity.setCreatedAt(route.getCreatedAt());
        entity.setUpdatedAt(route.getUpdatedAt());
        return entity;
    }

    public ManagedRoute toDomain() {
        return ManagedRoute.restore(
            id,
            serviceId,
            List.copyOf(paths),
            List.copyOf(hosts),
            List.copyOf(methods),
            stripPath,
            createdAt,
            updatedAt
        );
    }
}
