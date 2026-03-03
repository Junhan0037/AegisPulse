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
import java.time.Duration;
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

    @Test
    @DisplayName("5분 5xx 비율 5% 초과 + 최소 요청 200건이면 서비스를 격리 모드로 전환한다")
    void shouldSwitchServiceToIsolatedWhenIsolationTriggerConditionsAreMet() {
        Instant evaluatedAt = Instant.parse("2026-02-27T03:10:00Z");
        Alert active5xxAlert = Alert.newOpenAlert(
            "alt_open_5xx",
            AlertType.SERVICE_5XX_RATE_HIGH,
            "svc_01",
            Instant.parse("2026-02-27T03:05:00Z"),
            "{\"transition\":\"OPEN\"}"
        );

        given(managedServiceRepository.findAll()).willReturn(List.of(managedService("svc_01")));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willReturn(
                List.of(
                    MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:09:00Z"), 4.0, 100, 300, 0.2, 6.3)
                )
            );
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH))
            .willReturn(Optional.of(active5xxAlert));
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_P95_LATENCY_HIGH))
            .willReturn(Optional.empty());
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_ISOLATION_MODE))
            .willReturn(Optional.empty());
        given(alertRepository.save(any(Alert.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(managedServiceRepository.save(any(ManagedService.class))).willAnswer(invocation -> invocation.getArgument(0));

        alertEvaluationService.evaluateAt(evaluatedAt);

        ArgumentCaptor<ManagedService> serviceCaptor = ArgumentCaptor.forClass(ManagedService.class);
        then(managedServiceRepository).should().save(serviceCaptor.capture());
        assertThat(serviceCaptor.getValue().getStatus()).isEqualTo(ServiceStatus.ISOLATED);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        then(alertRepository).should().save(alertCaptor.capture());
        assertThat(alertCaptor.getValue().getAlertType()).isEqualTo(AlertType.SERVICE_ISOLATION_MODE);
        assertThat(alertCaptor.getValue().getState()).isEqualTo(AlertState.OPEN);
    }

    @Test
    @DisplayName("요청 수가 200건 미만이면 5xx 급증이어도 격리 모드로 전환하지 않는다")
    void shouldNotSwitchToIsolationWhenMinimumRequestCountIsNotMet() {
        Instant evaluatedAt = Instant.parse("2026-02-27T03:20:00Z");
        Alert active5xxAlert = Alert.newOpenAlert(
            "alt_open_5xx",
            AlertType.SERVICE_5XX_RATE_HIGH,
            "svc_01",
            Instant.parse("2026-02-27T03:15:00Z"),
            "{\"transition\":\"OPEN\"}"
        );

        given(managedServiceRepository.findAll()).willReturn(List.of(managedService("svc_01")));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willReturn(
                List.of(
                    MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:19:00Z"), 2.0, 100, 250, 0.1, 7.2)
                )
            );
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH))
            .willReturn(Optional.of(active5xxAlert));
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_P95_LATENCY_HIGH))
            .willReturn(Optional.empty());

        alertEvaluationService.evaluateAt(evaluatedAt);

        then(managedServiceRepository).should(never()).save(any(ManagedService.class));
        then(alertRepository).should(never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("연속 10분 5xx 비율이 모두 1% 미만이면 격리 모드를 자동 해제한다")
    void shouldRecoverServiceWhenTenMinuteWindowIsContinuouslyHealthy() {
        Instant evaluatedAt = Instant.parse("2026-02-27T04:00:00Z");
        ManagedService isolatedService = managedService("svc_01").isolate(Instant.parse("2026-02-27T03:40:00Z"));
        Alert isolationOpenAlert = Alert.newOpenAlert(
            "alt_isolation_open",
            AlertType.SERVICE_ISOLATION_MODE,
            "svc_01",
            Instant.parse("2026-02-27T03:40:00Z"),
            "{\"transition\":\"OPEN\"}"
        );

        List<MetricPoint> recentFiveMinutes = List.of(
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:59:00Z"), 3.5, 90, 200, 0.1, 0.4)
        );
        List<MetricPoint> recoveryWindow = List.of(
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:50:00Z"), 3.0, 90, 200, 0.1, 0.5),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:51:00Z"), 3.0, 90, 200, 0.1, 0.5),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:52:00Z"), 3.0, 90, 200, 0.1, 0.4),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:53:00Z"), 3.0, 90, 200, 0.1, 0.4),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:54:00Z"), 3.0, 90, 200, 0.1, 0.3),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:55:00Z"), 3.0, 90, 200, 0.1, 0.3),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:56:00Z"), 3.0, 90, 200, 0.1, 0.2),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:57:00Z"), 3.0, 90, 200, 0.1, 0.2),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:58:00Z"), 3.0, 90, 200, 0.1, 0.2),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T03:59:00Z"), 3.0, 90, 200, 0.1, 0.1)
        );

        given(managedServiceRepository.findAll()).willReturn(List.of(isolatedService));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willAnswer(invocation -> {
                Instant fromInclusive = invocation.getArgument(1);
                if (fromInclusive.equals(evaluatedAt.minus(Duration.ofMinutes(10)))) {
                    return recoveryWindow;
                }
                return recentFiveMinutes;
            });
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH))
            .willReturn(Optional.empty());
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_P95_LATENCY_HIGH))
            .willReturn(Optional.empty());
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_ISOLATION_MODE))
            .willReturn(Optional.of(isolationOpenAlert));
        given(alertRepository.save(any(Alert.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(managedServiceRepository.save(any(ManagedService.class))).willAnswer(invocation -> invocation.getArgument(0));

        alertEvaluationService.evaluateAt(evaluatedAt);

        ArgumentCaptor<ManagedService> serviceCaptor = ArgumentCaptor.forClass(ManagedService.class);
        then(managedServiceRepository).should().save(serviceCaptor.capture());
        assertThat(serviceCaptor.getValue().getStatus()).isEqualTo(ServiceStatus.ACTIVE);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        then(alertRepository).should().save(alertCaptor.capture());
        assertThat(alertCaptor.getValue().getAlertType()).isEqualTo(AlertType.SERVICE_ISOLATION_MODE);
        assertThat(alertCaptor.getValue().getState()).isEqualTo(AlertState.RESOLVED);
    }

    @Test
    @DisplayName("10분 구간 포인트가 연속되지 않으면 격리 모드를 유지한다")
    void shouldKeepIsolationWhenRecoveryWindowHasGap() {
        Instant evaluatedAt = Instant.parse("2026-02-27T04:10:00Z");
        ManagedService isolatedService = managedService("svc_01").isolate(Instant.parse("2026-02-27T03:40:00Z"));

        List<MetricPoint> recentFiveMinutes = List.of(
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:09:00Z"), 3.5, 90, 200, 0.1, 0.4)
        );
        List<MetricPoint> recoveryWindowWithGap = List.of(
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:00:00Z"), 3.0, 90, 200, 0.1, 0.5),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:01:00Z"), 3.0, 90, 200, 0.1, 0.5),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:02:00Z"), 3.0, 90, 200, 0.1, 0.4),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:03:00Z"), 3.0, 90, 200, 0.1, 0.4),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:04:00Z"), 3.0, 90, 200, 0.1, 0.3),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:06:00Z"), 3.0, 90, 200, 0.1, 0.3),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:07:00Z"), 3.0, 90, 200, 0.1, 0.2),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:08:00Z"), 3.0, 90, 200, 0.1, 0.2),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:09:00Z"), 3.0, 90, 200, 0.1, 0.2),
            MetricPoint.newPoint("svc_01", null, null, Instant.parse("2026-02-27T04:10:00Z"), 3.0, 90, 200, 0.1, 0.1)
        );

        given(managedServiceRepository.findAll()).willReturn(List.of(isolatedService));
        given(metricPointRepository.findByServiceIdAndWindow(eq("svc_01"), any(), eq(evaluatedAt)))
            .willAnswer(invocation -> {
                Instant fromInclusive = invocation.getArgument(1);
                if (fromInclusive.equals(evaluatedAt.minus(Duration.ofMinutes(10)))) {
                    return recoveryWindowWithGap;
                }
                return recentFiveMinutes;
            });
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_5XX_RATE_HIGH))
            .willReturn(Optional.empty());
        given(alertRepository.findActiveByTargetAndType("svc_01", AlertType.SERVICE_P95_LATENCY_HIGH))
            .willReturn(Optional.empty());

        alertEvaluationService.evaluateAt(evaluatedAt);

        then(managedServiceRepository).should(never()).save(any(ManagedService.class));
        then(alertRepository).should(never()).save(any(Alert.class));
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
