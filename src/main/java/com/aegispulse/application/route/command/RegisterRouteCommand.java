package com.aegispulse.application.route.command;

import com.aegispulse.domain.route.model.RouteHttpMethod;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Route 등록 유스케이스 입력 커맨드.
 */
@Getter
@Builder
public class RegisterRouteCommand {

    private final String serviceId;
    private final List<String> paths;
    private final List<String> hosts;
    private final List<RouteHttpMethod> methods;
    private final boolean stripPath;
    private final String actorId;
    private final String traceId;
}
