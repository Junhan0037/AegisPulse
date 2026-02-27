package com.aegispulse.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.domain.alert.repository.AlertRepository;
import com.aegispulse.domain.metric.model.MetricPoint;
import com.aegispulse.domain.metric.repository.MetricPointRepository;
import com.aegispulse.domain.service.model.ManagedService;
import com.aegispulse.domain.service.model.ServiceEnvironment;
import com.aegispulse.domain.service.model.ServiceStatus;
import com.aegispulse.domain.service.repository.ManagedServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationServiceTest {

    @Mock
    private ManagedServiceRepository managedServiceRepository;

    @Mock
    private MetricPointRepository metricPointRepository;

    @Mock
    private AlertRepository alertRepository;

    private AlertEvaluationService alertEvaluationService;

    @BeforeEach
    void setUp() {
        alertEvaluationService = new AlertEvaluationService(
            managedServiceRepository,
            metricPointRepository,
            alertRepository,
            new ObjectMapper()
        );
    }

    @Test
    @DisplayName("5xx 임계치 초과 시 OPEN 알림을 생성한다")
    void shouldCreateOpenAlertWhen5xxThresholdExceeded() {
        Instant evaluatedAt = Instant.parse("2026-02-27T02:00:00Z");
        given(managedServiceRepository.findAll()).willReturn(List.of(managedService("svc_01")));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willReturn(
                List.of(
                    MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T01:59:00Z"), 100, 200, 500, 0.3, 2.5)
                )
            );
        given(alertRepository.findActiveByTargetAndType(eq("svc_01"), any(AlertType.class))).willReturn(Optional.empty());
        given(alertRepository.findLatestByTargetAndType(eq("svc_01"), any(AlertType.class))).willReturn(Optional.empty());
        given(alertRepository.save(any(Alert.class))).willAnswer(invocation -> invocation.getArgument(0));

        alertEvaluationService.evaluateAt(evaluatedAt);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        then(alertRepository).should().save(alertCaptor.capture());
        Alert saved = alertCaptor.getValue();
        assertThat(saved.getTargetId()).isEqualTo("svc_01");
        assertThat(saved.getAlertType()).isEqualTo(AlertType.SERVICE_5XX_RATE_HIGH);
        assertThat(saved.getState()).isEqualTo(AlertState.OPEN);
    }

    @Test
    @DisplayName("cooldown 10분 이내 재초과는 신규 알림 생성을 억제한다")
    void shouldSuppressAlertCreationWithinCooldown() {
        Instant evaluatedAt = Instant.parse("2026-02-27T02:10:00Z");
        Alert latestResolved = Alert.restore(
            "alt_resolved",
            AlertType.SERVICE_5XX_RATE_HIGH,
            "svc_01",
            AlertState.RESOLVED,
            Instant.parse("2026-02-27T01:50:00Z"),
            Instant.parse("2026-02-27T02:05:00Z"),
            "{\"transition\":\"RESOLVED\"}"
        );

        given(managedServiceRepository.findAll()).willReturn(List.of(managedService("svc_01")));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willReturn(
                List.of(
                    MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T02:09:00Z"), 120, 100, 300, 0.4, 3.2)
                )
            );
        given(alertRepository.findActiveByTargetAndType(eq("svc_01"), any(AlertType.class))).willReturn(Optional.empty());
        given(alertRepository.findLatestByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH))
            .willReturn(Optional.of(latestResolved));

        alertEvaluationService.evaluateAt(evaluatedAt);

        then(alertRepository).should(never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("임계치 정상화 시 활성 알림을 RESOLVED로 전이한다")
    void shouldResolveActiveAlertWhenRecovered() {
        Instant evaluatedAt = Instant.parse("2026-02-27T03:00:00Z");
        Alert activeAlert = Alert.restore(
            "alt_open",
            AlertType.SERVICE_5XX_RATE_HIGH,
            "svc_01",
            AlertState.OPEN,
            Instant.parse("2026-02-27T02:50:00Z"),
            null,
            "{\"transition\":\"OPEN\"}"
        );

        given(managedServiceRepository.findAll()).willReturn(List.of(managedService("svc_01")));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willReturn(
                List.of(
                    MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T02:59:00Z"), 140, 80, 300, 0.2, 1.2)
                )
            );
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH))
            .willReturn(Optional.of(activeAlert));
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_P95_LATENCY_HIGH))
            .willReturn(Optional.empty());
        given(alertRepository.save(any(Alert.class))).willAnswer(invocation -> invocation.getArgument(0));

        alertEvaluationService.evaluateAt(evaluatedAt);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        then(alertRepository).should().save(alertCaptor.capture());
        Alert saved = alertCaptor.getValue();
        assertThat(saved.getId()).isEqualTo("alt_open");
        assertThat(saved.getState()).isEqualTo(AlertState.RESOLVED);
        assertThat(saved.getResolvedAt()).isEqualTo(evaluatedAt);
    }

    private ManagedService managedService(String serviceId) {
        return ManagedService.restore(
            serviceId,
            "payment-api",
            "https://payment.internal",
            ServiceEnvironment.PROD,
            ServiceStatus.ACTIVE,
            Instant.parse("2026-02-27T00:00:00Z"),
            Instant.parse("2026-02-27T00:00:00Z")
        );
    }
}
