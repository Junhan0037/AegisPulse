package com.aegispulse.infra.persistence.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.consumer.key.model.ManagedConsumerKey;
import com.aegispulse.infra.persistence.consumer.entity.ManagedConsumerKeyJpaEntity;
import com.aegispulse.infra.persistence.consumer.repository.ManagedConsumerKeyJpaRepository;
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

/**
 * ConsumerKey 저장 모델의 인덱스 반영 상태를 검증한다.
 * Stage 3의 ConsumerKey(consumerId) 조회 성능 의도를 테스트로 고정한다.
 */
@DataJpaTest
class ManagedConsumerKeyJpaEntityMappingTest {

    @Autowired
    private ManagedConsumerKeyJpaRepository managedConsumerKeyJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("ConsumerKey(consumerId) 인덱스가 스키마에 반영된다")
    void shouldExposeConsumerIdIndex() throws SQLException {
        managedConsumerKeyJpaRepository.saveAndFlush(
            ManagedConsumerKeyJpaEntity.fromDomain(
                ManagedConsumerKey.newActiveKey("key_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "csm_01", "pbkdf2$sample")
            )
        );

        Map<String, Set<String>> indexes = readIndexes("managed_consumer_keys", false);

        String indexName = indexes.keySet().stream()
            .filter(name -> name.startsWith("IDX_MANAGED_CONSUMER_KEYS_CONSUMER_ID"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("consumer_id 인덱스명을 찾을 수 없습니다: " + indexes.keySet()));

        assertThat(indexes.get(indexName)).contains("CONSUMER_ID");
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
