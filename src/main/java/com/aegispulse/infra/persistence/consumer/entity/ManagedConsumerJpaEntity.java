package com.aegispulse.infra.persistence.consumer.entity;

import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Consumer 영속화 JPA 엔티티.
 * PRD Stage 3의 Consumer(name) 전역 유니크 전략을 반영한다.
 */
@Entity
@Table(
    name = "managed_consumers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_managed_consumers_name",
            columnNames = {"name"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedConsumerJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsumerType type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static ManagedConsumerJpaEntity fromDomain(ManagedConsumer consumer) {
        ManagedConsumerJpaEntity entity = new ManagedConsumerJpaEntity();
        entity.setId(consumer.getId());
        entity.setName(consumer.getName());
        entity.setType(consumer.getType());
        entity.setCreatedAt(consumer.getCreatedAt());
        return entity;
    }

    public ManagedConsumer toDomain() {
        return ManagedConsumer.restore(
            id,
            name,
            type,
            createdAt
        );
    }
}
