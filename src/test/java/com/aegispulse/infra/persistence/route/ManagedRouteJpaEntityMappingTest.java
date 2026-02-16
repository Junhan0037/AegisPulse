package com.aegispulse.infra.persistence.route;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Route 저장 모델의 스키마 인덱스 반영 상태를 검증한다.
 * PRD Stage 1의 Route(serviceId) 인덱스 전략을 테스트로 고정한다.
 */
@DataJpaTest
class ManagedRouteJpaEntityMappingTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Route(serviceId) 인덱스가 스키마에 반영된다")
    void shouldExposeServiceIdIndex() throws SQLException {
        Map<String, Set<String>> indexes = readIndexes("managed_routes", false);

        // DB 구현체가 인덱스명에 접미사를 붙일 수 있어 prefix로 식별한다.
        String indexName = indexes.keySet().stream()
            .filter(name -> name.startsWith("IDX_MANAGED_ROUTES_SERVICE_ID"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("serviceId 인덱스명을 찾을 수 없습니다: " + indexes.keySet()));

        // 인덱스 대상 컬럼이 service_id인지 확인해 쿼리 최적화 의도를 보장한다.
        assertThat(indexes.get(indexName)).contains("SERVICE_ID");
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
