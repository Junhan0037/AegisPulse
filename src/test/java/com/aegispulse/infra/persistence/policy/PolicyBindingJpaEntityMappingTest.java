package com.aegispulse.infra.persistence.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.infra.persistence.policy.entity.PolicyBindingJpaEntity;
import com.aegispulse.infra.persistence.policy.repository.PolicyBindingJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * PolicyBinding 저장 모델의 매핑 규칙을 검증한다.
 */
@DataJpaTest
class PolicyBindingJpaEntityMappingTest {

    @Autowired
    private PolicyBindingJpaRepository policyBindingJpaRepository;

    @Test
    @DisplayName("템플릿 적용 바인딩이 routeId null 값을 포함해 정상 저장/조회된다")
    void shouldPersistAndLoadPolicyBindingWithNullableRouteId() {
        PolicyBinding domain = PolicyBinding.newBinding(
            "plb_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "svc_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            null,
            TemplateType.PUBLIC,
            "{\"templateType\":\"PUBLIC\"}",
            1
        );

        PolicyBindingJpaEntity saved = policyBindingJpaRepository.saveAndFlush(PolicyBindingJpaEntity.fromDomain(domain));
        PolicyBinding restored = saved.toDomain();

        assertThat(restored.getId()).isEqualTo("plb_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(restored.getServiceId()).isEqualTo("svc_01aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(restored.getRouteId()).isNull();
        assertThat(restored.getTemplateType()).isEqualTo(TemplateType.PUBLIC);
        assertThat(restored.getPolicySnapshot()).isEqualTo("{\"templateType\":\"PUBLIC\"}");
        assertThat(restored.getVersion()).isEqualTo(1);
        assertThat(restored.getCreatedAt()).isNotNull();
    }
}
