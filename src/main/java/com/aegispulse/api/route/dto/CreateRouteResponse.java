package com.aegispulse.api.route.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Route 등록 성공 응답 DTO.
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateRouteResponse {

    private final String routeId;
    private final String serviceId;
    private final List<String> paths;
    private final List<String> hosts;
    private final List<String> methods;
    private final boolean stripPath;
}
