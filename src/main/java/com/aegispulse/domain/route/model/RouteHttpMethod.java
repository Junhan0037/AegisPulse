package com.aegispulse.domain.route.model;

/**
 * FR-002에서 허용하는 HTTP Method 집합.
 * 허용되지 않은 메서드는 API 입력 단계에서 400으로 거부한다.
 */
public enum RouteHttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    HEAD
}
