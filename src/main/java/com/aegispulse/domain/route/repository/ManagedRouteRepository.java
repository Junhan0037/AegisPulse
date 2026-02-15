package com.aegispulse.domain.route.repository;

import com.aegispulse.domain.route.model.ManagedRoute;
import java.util.List;

/**
 * Route 도메인 저장소 추상화.
 */
public interface ManagedRouteRepository {

    /**
     * 특정 서비스에 속한 모든 라우트를 조회한다.
     */
    List<ManagedRoute> findAllByServiceId(String serviceId);

    /**
     * 라우트 도메인 모델을 저장한다.
     */
    ManagedRoute save(ManagedRoute route);
}
