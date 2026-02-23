package com.aegispulse.infra.persistence.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.domain.consumer.repository.ManagedConsumerRepository;
import com.aegispulse.infra.persistence.consumer.repository.ManagedConsumerRepositoryAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ManagedConsumerRepositoryAdapter.class)
class ManagedConsumerRepositoryAdapterTest {

    @Autowired
    private ManagedConsumerRepository managedConsumerRepository;

    @Test
    @DisplayName("Consumer 저장 후 existsByName 조회가 true를 반환한다")
    void shouldSaveAndCheckNameDuplication() {
        ManagedConsumer saved = managedConsumerRepository.save(
            ManagedConsumer.newConsumer("csm_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "partner-client-a", ConsumerType.PARTNER)
        );

        boolean exists = managedConsumerRepository.existsByName("partner-client-a");

        assertThat(saved.getId()).isEqualTo("csm_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(saved.getName()).isEqualTo("partner-client-a");
        assertThat(saved.getType()).isEqualTo(ConsumerType.PARTNER);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(exists).isTrue();
        assertThat(managedConsumerRepository.findById("csm_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).isPresent();
    }
}
