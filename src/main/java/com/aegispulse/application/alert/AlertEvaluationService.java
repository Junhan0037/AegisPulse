package com.aegispulse.application.alert;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.domain.alert.repository.AlertRepository;
import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-007 임계치 평가/상태 전이 담당 서비스.
 */
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private static final Duration WINDOW_DURATION = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(10);
    private static final double THRESHOLD_5XX_RATE = 2.0;
    private static final double THRESHOLD_P95_LATENCY_MS = 800.0;

    private final ManagedServiceRepository managedServiceRepository;
    private final MetricPointRepository metricPointRepository;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    /**
     * 스케줄러 진입점.
     */
    @Transactional
    public void evaluate() {
        evaluateAt(Instant.now());
    }

    /**
     * 테스트/재평가 시각 주입을 위한 내부 진입점.
     */
    void evaluateAt(Instant evaluatedAt) {
        List<ManagedService> services = managedServiceRepository.findAll();
        for (ManagedService service : services) {
            evaluateService(service.getId(), evaluatedAt);
        }
    }

    private void evaluateService(String serviceId, Instant evaluatedAt) {
        Instant fromInclusive = evaluatedAt.minus(WINDOW_DURATION);
        List<MetricPoint> serviceAxisPoints = metricPointRepository.findByServiceIdAndWindow(
            serviceId,
            fromInclusive,
            evaluatedAt
        ).stream()
            // Stage 5 평가는 서비스 축으로 고정한다.
            .filter(point -> point.getRouteId() == null && point.getConsumerId() == null)
            .toList();

        // 수집 공백 구간에서는 상태를 강제로 바꾸지 않는다.
        if (serviceAxisPoints.isEmpty()) {
            return;
        }

        double observed5xxRate = weightedAverage(serviceAxisPoints, MetricPoint::getStatus5xxRate);
        double observedP95Latency = weightedAverage(serviceAxisPoints, MetricPoint::getLatencyP95);

        evaluateRule(
            serviceId,
            AlertType.SERVICE_5XX_RATE_HIGH,
            observed5xxRate > THRESHOLD_5XX_RATE,
            observed5xxRate,
            THRESHOLD_5XX_RATE,
            evaluatedAt
        );
        evaluateRule(
            serviceId,
            AlertType.SERVICE_P95_LATENCY_HIGH,
            observedP95Latency > THRESHOLD_P95_LATENCY_MS,
            observedP95Latency,
            THRESHOLD_P95_LATENCY_MS,
            evaluatedAt
        );
    }

    private void evaluateRule(
        String serviceId,
        AlertType alertType,
        boolean breached,
        double observedValue,
        double threshold,
        Instant evaluatedAt
    ) {
        Optional<Alert> activeAlert = alertRepository.findActiveByTargetAndType(serviceId, alertType);

        if (breached) {
            if (activeAlert.isPresent()) {
                return;
            }

            if (isInCooldown(serviceId, alertType, evaluatedAt)) {
                return;
            }

            Alert openAlert = Alert.newOpenAlert(
                null,
                alertType,
                serviceId,
                evaluatedAt,
                buildPayloadJson(serviceId, alertType, observedValue, threshold, "OPEN", evaluatedAt)
            );
            alertRepository.save(openAlert);
            return;
        }

        if (activeAlert.isPresent()) {
            Alert resolvedAlert;
            try {
                resolvedAlert = activeAlert.get().resolve(
                    evaluatedAt,
                    buildPayloadJson(serviceId, alertType, observedValue, threshold, "RESOLVED", evaluatedAt)
                );
            } catch (IllegalStateException exception) {
                throw new AegisPulseException(ErrorCode.ALERT_STATE_CONFLICT, exception.getMessage());
            }
            alertRepository.save(resolvedAlert);
        }
    }

    private boolean isInCooldown(String serviceId, AlertType alertType, Instant evaluatedAt) {
        Optional<Alert> latestAlert = alertRepository.findLatestByTargetAndType(serviceId, alertType);
        if (latestAlert.isEmpty()) {
            return false;
        }

        Alert latest = latestAlert.get();
        if (latest.getState() != AlertState.RESOLVED || latest.getResolvedAt() == null) {
            return false;
        }

        return latest.getResolvedAt().plus(COOLDOWN_DURATION).isAfter(evaluatedAt);
    }

    private double weightedAverage(List<MetricPoint> points, ToDoubleFunction<MetricPoint> extractor) {
        double totalWeight = points.stream().mapToDouble(MetricPoint::getRps).sum();
        if (totalWeight <= 0) {
            return points.stream().mapToDouble(extractor).average().orElse(0);
        }
        double weightedSum = points.stream()
            .mapToDouble(point -> extractor.applyAsDouble(point) * point.getRps())
            .sum();
        return weightedSum / totalWeight;
    }

    private String buildPayloadJson(
        String serviceId,
        AlertType alertType,
        double observedValue,
        double threshold,
        String transition,
        Instant evaluatedAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("serviceId", serviceId);
        payload.put("alertType", alertType.name());
        payload.put("window", "5m");
        payload.put("threshold", threshold);
        payload.put("observedValue", observedValue);
        payload.put("transition", transition);
        payload.put("cooldownMinutes", COOLDOWN_DURATION.toMinutes());
        payload.put("evaluatedAt", evaluatedAt.toString());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new AegisPulseException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "알림 payload 직렬화에 실패했습니다."
            );
        }
    }
}
