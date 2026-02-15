package com.aegispulse.application.route.result;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Route 등록 유스케이스 출력 모델.
 */
@Getter
@Builder
public class RegisterRouteResult {

    private final String routeId;
    private final String serviceId;
    private final List<String> paths;
    private final List<String> hosts;
    private final List<String> methods;
    private final boolean stripPath;
}
