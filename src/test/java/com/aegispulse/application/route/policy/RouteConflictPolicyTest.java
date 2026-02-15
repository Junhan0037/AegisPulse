package com.aegispulse.application.route.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.model.RouteHttpMethod;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteConflictPolicyTest {

    private final RouteConflictPolicy routeConflictPolicy = new RouteConflictPolicy();

    @Test
    @DisplayName("path가 같고 hosts/methods가 와일드카드 교집합이면 충돌한다")
    void shouldConflictWhenPathMatchesAndWildcardApplies() {
        ManagedRoute candidate = route(
            "rte_candidate",
            "svc_01",
            List.of("/payments"),
            List.of(),
            List.of(RouteHttpMethod.GET),
            true
        );
        ManagedRoute existing = route(
            "rte_existing",
            "svc_01",
            List.of("/payments"),
            List.of("api.partner.com"),
            List.of(),
            true
        );

        assertThat(routeConflictPolicy.hasConflict(candidate, List.of(existing))).isTrue();
    }

    @Test
    @DisplayName("path가 같아도 method 교집합이 없으면 충돌하지 않는다")
    void shouldNotConflictWhenMethodsDoNotOverlap() {
        ManagedRoute candidate = route(
            "rte_candidate",
            "svc_01",
            List.of("/payments"),
            List.of("api.partner.com"),
            List.of(RouteHttpMethod.GET),
            true
        );
        ManagedRoute existing = route(
            "rte_existing",
            "svc_01",
            List.of("/payments"),
            List.of("api.partner.com"),
            List.of(RouteHttpMethod.POST),
            true
        );

        assertThat(routeConflictPolicy.hasConflict(candidate, List.of(existing))).isFalse();
    }

    @Test
    @DisplayName("path 교집합이 없으면 hosts/methods가 같아도 충돌하지 않는다")
    void shouldNotConflictWhenPathsDoNotOverlap() {
        ManagedRoute candidate = route(
            "rte_candidate",
            "svc_01",
            List.of("/payments"),
            List.of("api.partner.com"),
            List.of(RouteHttpMethod.GET),
            true
        );
        ManagedRoute existing = route(
            "rte_existing",
            "svc_01",
            List.of("/orders"),
            List.of("api.partner.com"),
            List.of(RouteHttpMethod.GET),
            true
        );

        assertThat(routeConflictPolicy.hasConflict(candidate, List.of(existing))).isFalse();
    }

    private ManagedRoute route(
        String id,
        String serviceId,
        List<String> paths,
        List<String> hosts,
        List<RouteHttpMethod> methods,
        boolean stripPath
    ) {
        return ManagedRoute.newRoute(id, serviceId, paths, hosts, methods, stripPath);
    }
}
