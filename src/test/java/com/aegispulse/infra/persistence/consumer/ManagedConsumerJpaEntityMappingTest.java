package com.aegispulse.infra.persistence.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aegispulse.domain.consumer.model.ConsumerType;
import com.aegispulse.domain.consumer.model.ManagedConsumer;
import com.aegispulse.infra.persistence.consumer.entity.ManagedConsumerJpaEntity;
import com.aegispulse.infra.persistence.consumer.repository.ManagedConsumerJpaRepository;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Consumer 저장 모델의 스키마 반영 상태를 검증한다.
 * Stage 3의 Consumer(name) 유니크 전략을 테스트로 고정한다.
 */
@DataJpaTest
class ManagedConsumerJpaEntityMappingTest {

    @Autowired
    private ManagedConsumerJpaRepository managedConsumerJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Consumer(name) 유니크 제약 이름과 컬럼 구성이 스키마에 반영된다")
    void shouldExposeConfiguredNameUniqueConstraint() throws SQLException {
        Map<String, Set<String>> uniqueIndexes = readIndexes("managed_consumers", true);

        // H2는 유니크 제약 인덱스명에 접미사를 부여할 수 있어 prefix로 식별한다.
        String uniqueIndexName = uniqueIndexes.keySet().stream()
            .filter(name -> name.startsWith("UK_MANAGED_CONSUMERS_NAME"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("유니크 인덱스명을 찾을 수 없습니다: " + uniqueIndexes.keySet()));

        assertThat(uniqueIndexes.get(uniqueIndexName)).containsExactly("NAME");
    }

    @Test
    @DisplayName("동일 name 중복 저장 시 유니크 제약 위반이 발생한다")
    void shouldFailWhenNameIsDuplicated() {
        managedConsumerJpaRepository.saveAndFlush(consumerEntity(
            "csm_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "partner-client-a",
            ConsumerType.PARTNER
        ));

        ManagedConsumerJpaEntity duplicated = consumerEntity(
            "csm_02bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "partner-client-a",
            ConsumerType.INTERNAL
        );

        // 애플리케이션 선조회가 누락되더라도 DB 유니크 제약으로 무결성이 보장돼야 한다.
        assertThatThrownBy(() -> managedConsumerJpaRepository.saveAndFlush(duplicated))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ManagedConsumerJpaEntity consumerEntity(String id, String name, ConsumerType type) {
        ManagedConsumer domain = ManagedConsumer.newConsumer(id, name, type);
        return ManagedConsumerJpaEntity.fromDomain(domain);
    }

    private Map<String, Set<String>> readIndexes(String tableName, boolean uniqueOnly) throws SQLException {
        Map<String, Set<String>> indexes = new LinkedHashMap<>();
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getIndexInfo(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                uniqueOnly,
                false
            )) {
                while (resultSet.next()) {
                    String indexName = resultSet.getString("INDEX_NAME");
                    String columnName = resultSet.getString("COLUMN_NAME");
                    if (indexName == null || columnName == null) {
                        continue;
                    }

                    String normalizedIndexName = indexName.toUpperCase(Locale.ROOT);
                    String normalizedColumnName = columnName.toUpperCase(Locale.ROOT);
                    indexes.computeIfAbsent(normalizedIndexName, key -> new LinkedHashSet<>())
                        .add(normalizedColumnName);
                }
            }
        }
        return indexes;
    }
}
