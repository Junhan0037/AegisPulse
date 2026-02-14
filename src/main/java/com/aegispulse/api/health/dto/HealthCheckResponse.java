package com.aegispulse.api.health.dto;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 기본 헬스체크 응답 데이터.
 * Stage 0에서는 애플리케이션 생존 여부를 단순 상태(UP)로 반환한다.
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HealthCheckResponse {

    private final String status;
    private final String service;
    private final Instant checkedAt;
}
