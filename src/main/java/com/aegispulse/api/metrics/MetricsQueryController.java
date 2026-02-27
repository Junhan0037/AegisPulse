package com.aegispulse.api.metrics;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.api.common.response.ApiResponse;
import com.aegispulse.api.metrics.dto.ConsumerMetricResponse;
import com.aegispulse.api.metrics.dto.MetricAggregateResponse;
import com.aegispulse.api.metrics.dto.QueryServiceMetricsResponse;
import com.aegispulse.api.metrics.dto.RouteMetricResponse;
import com.aegispulse.api.metrics.dto.TopRouteMetricResponse;
import com.aegispulse.application.metric.MetricQueryUseCase;
import com.aegispulse.application.metric.command.QueryServiceMetricsCommand;
import com.aegispulse.application.metric.result.ConsumerMetricResult;
import com.aegispulse.application.metric.result.MetricAggregateResult;
import com.aegispulse.application.metric.result.QueryServiceMetricsResult;
import com.aegispulse.application.metric.result.RouteMetricResult;
import com.aegispulse.application.metric.result.TopRouteMetricResult;
import com.aegispulse.domain.metric.model.MetricWindow;
import com.aegispulse.infra.web.trace.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 기준 메트릭 조회 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/metrics/services")
@RequiredArgsConstructor
public class MetricsQueryController {

    private final MetricQueryUseCase metricQueryUseCase;

    @GetMapping("/{serviceId}")
    public ResponseEntity<ApiResponse<QueryServiceMetricsResponse>> getServiceMetrics(
        @PathVariable String serviceId,
        @RequestParam(required = false) String window,
        HttpServletRequest httpServletRequest
    ) {
        if (!StringUtils.hasText(serviceId)) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "serviceId는 필수입니다.");
        }

        MetricWindow metricWindow = MetricWindow.fromQuery(window);
        QueryServiceMetricsResult result = metricQueryUseCase.queryServiceMetrics(
            QueryServiceMetricsCommand.builder()
                .serviceId(serviceId.trim())
                .window(metricWindow)
                .build()
        );

        QueryServiceMetricsResponse response = QueryServiceMetricsResponse.builder()
            .serviceId(result.getServiceId())
            .window(result.getWindow())
            .service(toAggregateResponse(result.getServiceMetrics()))
            .routeMetrics(toRouteResponses(result.getRouteMetrics()))
            .consumerMetrics(toConsumerResponses(result.getConsumerMetrics()))
            .topRoutes(toTopRouteResponses(result.getTopRoutes()))
            .build();

        return ResponseEntity.ok(ApiResponse.success(response, resolveTraceId(httpServletRequest)));
    }

    private List<RouteMetricResponse> toRouteResponses(List<RouteMetricResult> routeMetrics) {
        return routeMetrics.stream()
            .map(routeMetric -> RouteMetricResponse.builder()
                .routeId(routeMetric.getRouteId())
                .metrics(toAggregateResponse(routeMetric.getMetrics()))
                .build())
            .toList();
    }

    private List<ConsumerMetricResponse> toConsumerResponses(List<ConsumerMetricResult> consumerMetrics) {
        return consumerMetrics.stream()
            .map(consumerMetric -> ConsumerMetricResponse.builder()
                .consumerId(consumerMetric.getConsumerId())
                .metrics(toAggregateResponse(consumerMetric.getMetrics()))
                .build())
            .toList();
    }

    private List<TopRouteMetricResponse> toTopRouteResponses(List<TopRouteMetricResult> topRoutes) {
        return topRoutes.stream()
            .map(topRoute -> TopRouteMetricResponse.builder()
                .routeId(topRoute.getRouteId())
                .rps(topRoute.getRps())
                .status5xxRate(topRoute.getStatus5xxRate())
                .latencyP95(topRoute.getLatencyP95())
                .build())
            .toList();
    }

    private MetricAggregateResponse toAggregateResponse(MetricAggregateResult source) {
        return MetricAggregateResponse.builder()
            .rps(source.getRps())
            .status4xxRate(source.getStatus4xxRate())
            .status5xxRate(source.getStatus5xxRate())
            .latencyP50(source.getLatencyP50())
            .latencyP95(source.getLatencyP95())
            .build();
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 필터 체인이 비정상 동작해도 응답 traceId를 보장해 클라이언트 디버깅 가능성을 유지한다.
        Object traceIdAttribute = request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        return TraceIdSupport.generate();
    }
}
