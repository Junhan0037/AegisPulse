package com.aegispulse.application.route;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.route.command.RegisterRouteCommand;
import com.aegispulse.application.route.policy.RouteConflictPolicy;
import com.aegispulse.application.route.result.RegisterRouteResult;
import com.aegispulse.domain.route.model.ManagedRoute;
import com.aegispulse.domain.route.repository.ManagedRouteRepository;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Route 등록 유스케이스 구현체.
 * 서비스 존재성 검증과 충돌 검증 후 신규 Route를 저장한다.
 */
@Service
@RequiredArgsConstructor
public class RouteRegistrationService implements RouteRegistrationUseCase {

    private final ManagedServiceRepository managedServiceRepository;
    private final ManagedRouteRepository managedRouteRepository;
    private final RouteConflictPolicy routeConflictPolicy;

    @Override
    @Transactional
    public RegisterRouteResult register(RegisterRouteCommand command) {
        if (!managedServiceRepository.existsById(command.getServiceId())) {
            throw new AegisPulseException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 서비스를 찾을 수 없습니다.");
        }

        ManagedRoute candidate = ManagedRoute.newRoute(
            generateRouteId(),
            command.getServiceId(),
            command.getPaths(),
            command.getHosts(),
            command.getMethods(),
            command.isStripPath()
        );

        List<ManagedRoute> existingRoutes = managedRouteRepository.findAllByServiceId(command.getServiceId());
        if (routeConflictPolicy.hasConflict(candidate, existingRoutes)) {
            throw new AegisPulseException(ErrorCode.ROUTE_CONFLICT, "동일 서비스 내 충돌하는 라우트입니다.");
        }

        ManagedRoute saved = managedRouteRepository.save(candidate);
        return RegisterRouteResult.builder()
            .routeId(saved.getId())
            .serviceId(saved.getServiceId())
            .paths(saved.getPaths())
            .hosts(saved.getHosts())
            .methods(saved.getMethods().stream().map(Enum::name).toList())
            .stripPath(saved.isStripPath())
            .build();
    }

    private String generateRouteId() {
        return "rte_" + UUID.randomUUID().toString().replace("-", "");
    }
}
