package com.aegispulse.api.metrics;

import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.metrics.dto.IngestMetricPointRequest;
import com.aegispulse.api.metrics.dto.IngestMetricPointsRequest;
import com.aegispulse.api.metrics.dto.IngestMetricPointsResponse;
import com.aegispulse.application.metric.MetricIngestionUseCase;
import com.aegispulse.application.metric.command.IngestMetricPointsCommand;
import com.aegispulse.application.metric.command.MetricPointIngestItem;
import com.aegispulse.application.metric.result.IngestMetricPointsResult;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메트릭 수집 파이프라인 입력 컨트롤러.
 * Collector가 전달한 집계 포인트를 애플리케이션 계층으로 전달한다.
 */
@RestController
@RequestMapping("/api/v1/internal/metrics/points:batch")
@RequiredArgsConstructor
public class InternalMetricsIngestionController {

    private final MetricIngestionUseCase metricIngestionUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<IngestMetricPointsResponse>> ingest(
        @Valid @RequestBody IngestMetricPointsRequest request,
        HttpServletRequest httpServletRequest
    ) {
        List<MetricPointIngestItem> items = request.getPoints().stream()
            .map(this::toCommandItem)
            .toList();
        IngestMetricPointsResult result = metricIngestionUseCase.ingest(
            IngestMetricPointsCommand.builder()
                .points(items)
                .build()
        );

        IngestMetricPointsResponse response = IngestMetricPointsResponse.builder()
            .ingestedCount(result.getIngestedCount())
            .build();
        return ResponseEntity.ok(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
    }

    private MetricPointIngestItem toCommandItem(IngestMetricPointRequest point) {
        return MetricPointIngestItem.builder()
            .serviceId(point.getServiceId().trim())
            .routeId(normalizeOptional(point.getRouteId()))
            .consumerId(normalizeOptional(point.getConsumerId()))
            .windowStart(point.getWindowStart())
            .rps(point.getRps())
            .latencyP50(point.getLatencyP50())
            .latencyP95(point.getLatencyP95())
            .status4xxRate(point.getStatus4xxRate())
            .status5xxRate(point.getStatus5xxRate())
            .build();
    }

    private String normalizeOptional(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 필터 체인이 비정상 동작해도 응답 traceId를 보장해 파이프라인 추적성을 유지한다.
        Object traceIdAttribute = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        return TraceIdSupport.generate();
    }
}
