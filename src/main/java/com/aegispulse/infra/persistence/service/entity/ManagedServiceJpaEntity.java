package com.aegispulse.infra.persistence.service.entity;

import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;
import com.aegispulse.domain.service.model.ServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Service 영속화 JPA 엔티티.
 * PRD 인덱스 전략 중 Service(environment, name) 유니크 제약을 반영한다.
 */
@Entity
@Table(
    name = "managed_services",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_managed_services_environment_name",
            columnNames = {"environment", "name"}
        )
    },
    indexes = {
        @Index(name = "idx_managed_services_environment", columnList = "environment")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedServiceJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 2048)
    private String upstreamUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ServiceEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * 생성 시각/상태 기본값을 보장한다.
     */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = ServiceStatus.ACTIVE;
        }
        updatedAt = now;
    }

    /**
     * 수정 시각을 자동 갱신한다.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static ManagedServiceJpaEntity fromDomain(ManagedService service) {
        ManagedServiceJpaEntity entity = new ManagedServiceJpaEntity();
        entity.setId(service.getId());
        entity.setName(service.getName());
        entity.setUpstreamUrl(service.getUpstreamUrl());
        entity.setEnvironment(service.getEnvironment());
        entity.setStatus(service.getStatus());
        entity.setCreatedAt(service.getCreatedAt());
        entity.setUpdatedAt(service.getUpdatedAt());
        return entity;
    }

    public ManagedService toDomain() {
        return ManagedService.restore(
            id,
            name,
            upstreamUrl,
            environment,
            status,
            createdAt,
            updatedAt
        );
    }
}
