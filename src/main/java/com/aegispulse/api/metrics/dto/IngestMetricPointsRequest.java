package com.aegispulse.api.metrics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메트릭 포인트 배치 입력 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class IngestMetricPointsRequest {

    @NotEmpty(message = "points는 최소 1개 이상이어야 합니다.")
    private List<@Valid IngestMetricPointRequest> points;
}
