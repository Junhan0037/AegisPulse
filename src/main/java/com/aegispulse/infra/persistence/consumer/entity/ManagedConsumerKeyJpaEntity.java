package com.aegispulse.infra.persistence.consumer.entity;

import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
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
 * Consumer API Key 영속화 JPA 엔티티.
 */
@Entity
@Table(
    name = "managed_consumer_keys",
    indexes = {
        @Index(name = "idx_managed_consumer_keys_consumer_id", columnList = "consumer_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedConsumerKeyJpaEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(name = "consumer_id", nullable = false, length = 40)
    private String consumerId;

    @Column(name = "key_hash", nullable = false, length = 512)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsumerKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static ManagedConsumerKeyJpaEntity fromDomain(ManagedConsumerKey consumerKey) {
        ManagedConsumerKeyJpaEntity entity = new ManagedConsumerKeyJpaEntity();
        entity.setId(consumerKey.getId());
        entity.setConsumerId(consumerKey.getConsumerId());
        entity.setKeyHash(consumerKey.getKeyHash());
        entity.setStatus(consumerKey.getStatus());
        entity.setCreatedAt(consumerKey.getCreatedAt());
        entity.setRevokedAt(consumerKey.getRevokedAt());
        return entity;
    }

    public ManagedConsumerKey toDomain() {
        return ManagedConsumerKey.restore(
            id,
            consumerId,
            keyHash,
            status,
            createdAt,
            revokedAt
        );
    }
}
