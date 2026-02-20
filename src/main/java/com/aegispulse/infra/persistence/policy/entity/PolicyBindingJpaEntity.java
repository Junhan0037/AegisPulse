package com.aegispulse.infra.persistence.policy.entity;

import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PolicyBinding 영속화 JPA 엔티티.
 * 템플릿 적용 결과 스냅샷을 변경 불가능 이력 형태로 저장한다.
 */
@Entity
@Table(
    name = "policy_bindings",
    indexes = {
        @Index(name = "idx_policy_bindings_service_id", columnList = "service_id"),
        @Index(name = "idx_policy_bindings_route_id", columnList = "route_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyBindingJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(name = "service_id", nullable = false, length = 40)
    private String serviceId;

    @Column(name = "route_id", length = 40)
    private String routeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 20)
    private TemplateType templateType;

    @Column(name = "policy_snapshot", nullable = false, columnDefinition = "TEXT")
    private String policySnapshot;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 생성 시각이 비어 있으면 현재 시각으로 보정한다.
     */
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static PolicyBindingJpaEntity fromDomain(PolicyBinding policyBinding) {
        PolicyBindingJpaEntity entity = new PolicyBindingJpaEntity();
        entity.setId(policyBinding.getId());
        entity.setServiceId(policyBinding.getServiceId());
        entity.setRouteId(policyBinding.getRouteId());
        entity.setTemplateType(policyBinding.getTemplateType());
        entity.setPolicySnapshot(policyBinding.getPolicySnapshot());
        entity.setVersion(policyBinding.getVersion());
        entity.setCreatedAt(policyBinding.getCreatedAt());
        return entity;
    }

    public PolicyBinding toDomain() {
        return PolicyBinding.restore(
            id,
            serviceId,
            routeId,
            templateType,
            policySnapshot,
            version,
            createdAt
        );
    }
}
