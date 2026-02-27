package com.aegispulse.api.metrics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메트릭 포인트 단건 입력 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class IngestMetricPointRequest {

    @NotBlank(message = "serviceId는 필수입니다.")
    private String serviceId;

    private String routeId;

    private String consumerId;

    @NotNull(message = "windowStart는 필수입니다.")
    private Instant windowStart;

    @PositiveOrZero(message = "rps는 0 이상이어야 합니다.")
    private double rps;

    @PositiveOrZero(message = "latencyP50은 0 이상이어야 합니다.")
    private double latencyP50;

    @PositiveOrZero(message = "latencyP95는 0 이상이어야 합니다.")
    private double latencyP95;

    @PositiveOrZero(message = "status4xxRate는 0 이상이어야 합니다.")
    private double status4xxRate;

    @PositiveOrZero(message = "status5xxRate는 0 이상이어야 합니다.")
    private double status5xxRate;
}
