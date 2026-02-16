package com.aegispulse.infra.persistence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;
import com.aegispulse.infra.persistence.service.entity.ManagedServiceJpaEntity;
import com.aegispulse.infra.persistence.service.repository.ManagedServiceJpaRepository;
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
 * Service 저장 모델의 스키마 반영 상태를 검증한다.
 * PRD Stage 1의 Service(environment, name) 유니크 전략을 테스트로 고정한다.
 */
@DataJpaTest
class ManagedServiceJpaEntityMappingTest {

    @Autowired
    private ManagedServiceJpaRepository managedServiceJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Service(environment, name) 유니크 제약 이름과 컬럼 구성이 스키마에 반영된다")
    void shouldExposeConfiguredCompositeUniqueConstraint() throws SQLException {
        Map<String, Set<String>> uniqueIndexes = readIndexes("managed_services", true);

        // H2는 유니크 제약 인덱스명에 접미사를 부여할 수 있어 prefix로 식별한다.
        String uniqueIndexName = uniqueIndexes.keySet().stream()
            .filter(name -> name.startsWith("UK_MANAGED_SERVICES_ENVIRONMENT_NAME"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("유니크 인덱스명을 찾을 수 없습니다: " + uniqueIndexes.keySet()));

        // 유니크 제약 대상이 environment + name 조합인지 검증한다.
        assertThat(uniqueIndexes.get(uniqueIndexName))
            .containsExactlyInAnyOrder("ENVIRONMENT", "NAME");
    }

    @Test
    @DisplayName("동일 environment + name 중복 저장 시 유니크 제약 위반이 발생한다")
    void shouldFailWhenEnvironmentAndNameAreDuplicated() {
        managedServiceJpaRepository.saveAndFlush(serviceEntity(
            "svc_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "partner-payment-api",
            ServiceEnvironment.PROD
        ));

        ManagedServiceJpaEntity duplicated = serviceEntity(
            "svc_02bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "partner-payment-api",
            ServiceEnvironment.PROD
        );

        // 애플리케이션 선조회가 누락되더라도 DB 유니크 제약으로 최종 무결성이 보장돼야 한다.
        assertThatThrownBy(() -> managedServiceJpaRepository.saveAndFlush(duplicated))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ManagedServiceJpaEntity serviceEntity(String id, String name, ServiceEnvironment environment) {
        // protected 기본 생성자에 직접 의존하지 않도록 도메인 모델을 통해 엔티티를 생성한다.
        ManagedService domain = ManagedService.newService(id, name, "https://payment.internal.svc", environment);
        return ManagedServiceJpaEntity.fromDomain(domain);
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
