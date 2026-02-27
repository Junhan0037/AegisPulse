package com.aegispulse.api.metrics.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 메트릭 포인트 배치 수집 응답 DTO.
 */
@Getter
@Builder
public class IngestMetricPointsResponse {

    private final int ingestedCount;
}
