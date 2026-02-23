package com.aegispulse.infra.persistence.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.consumer.key.model.ConsumerKeyStatus;
import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
import com.aegispulse.domain.consumer.key.repository.ManagedConsumerKeyRepository;
import com.aegispulse.infra.persistence.consumer.repository.ManagedConsumerKeyRepositoryAdapter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ManagedConsumerKeyRepositoryAdapter.class)
class ManagedConsumerKeyRepositoryAdapterTest {

    @Autowired
    private ManagedConsumerKeyRepository managedConsumerKeyRepository;

    @Test
    @DisplayName("ACTIVE 키 조회 시 폐기된 키는 제외된다")
    void shouldFindOnlyActiveKeysByConsumerId() {
        managedConsumerKeyRepository.save(
            ManagedConsumerKey.newActiveKey("key_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "csm_01", "pbkdf2$active")
        );
        managedConsumerKeyRepository.save(
            ManagedConsumerKey.newActiveKey("key_02bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "csm_01", "pbkdf2$revoked").revoke()
        );

        List<ManagedConsumerKey> activeKeys = managedConsumerKeyRepository.findAllByConsumerIdAndStatus(
            "csm_01",
            ConsumerKeyStatus.ACTIVE
        );

        assertThat(activeKeys).hasSize(1);
        assertThat(activeKeys.getFirst().getId()).isEqualTo("key_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(activeKeys.getFirst().getStatus()).isEqualTo(ConsumerKeyStatus.ACTIVE);
    }
}
