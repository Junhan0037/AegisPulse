package com.aegispulse.infra.persistence.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.policy.model.PolicyBinding;
import com.aegispulse.domain.policy.model.TemplateType;
import com.aegispulse.domain.policy.repository.PolicyBindingRepository;
import com.aegispulse.infra.persistence.policy.repository.PolicyBindingRepositoryAdapter;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(PolicyBindingRepositoryAdapter.class)
class PolicyBindingRepositoryAdapterTest {

    @Autowired
    private PolicyBindingRepository policyBindingRepository;

    @Test
    @DisplayName("routeId가 있으면 서비스/라우트 조합의 최신 버전을 조회한다")
    void shouldFindLatestRouteBinding() {
        policyBindingRepository.save(binding("plb_01", "svc_01", "rte_01", TemplateType.PUBLIC, 1));
        policyBindingRepository.save(binding("plb_02", "svc_01", "rte_01", TemplateType.INTERNAL, 3));
        policyBindingRepository.save(binding("plb_03", "svc_01", "rte_02", TemplateType.PARTNER, 7));

        PolicyBinding latest = policyBindingRepository.findLatest("svc_01", "rte_01").orElseThrow();

        assertThat(latest.getId()).isEqualTo("plb_02");
        assertThat(latest.getVersion()).isEqualTo(3);
        assertThat(latest.getTemplateType()).isEqualTo(TemplateType.INTERNAL);
    }

    @Test
    @DisplayName("routeId가 null이면 서비스 단위 정책의 최신 버전을 조회한다")
    void shouldFindLatestServiceBindingWhenRouteIdIsNull() {
        policyBindingRepository.save(binding("plb_11", "svc_01", null, TemplateType.PUBLIC, 1));
        policyBindingRepository.save(binding("plb_12", "svc_01", null, TemplateType.PARTNER, 4));
        policyBindingRepository.save(binding("plb_13", "svc_01", "rte_01", TemplateType.INTERNAL, 10));

        PolicyBinding latest = policyBindingRepository.findLatest("svc_01", null).orElseThrow();

        assertThat(latest.getId()).isEqualTo("plb_12");
        assertThat(latest.getRouteId()).isNull();
        assertThat(latest.getVersion()).isEqualTo(4);
        assertThat(latest.getTemplateType()).isEqualTo(TemplateType.PARTNER);
    }

    private PolicyBinding binding(
        String id,
        String serviceId,
        String routeId,
        TemplateType templateType,
        int version
    ) {
        return PolicyBinding.restore(
            id,
            serviceId,
            routeId,
            templateType,
            "{\"templateType\":\"" + templateType.name() + "\"}",
            version,
            Instant.parse("2026-02-20T12:00:00Z").plusSeconds(version)
        );
    }
}
