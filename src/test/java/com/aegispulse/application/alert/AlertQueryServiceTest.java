package com.aegispulse.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.alert.command.QueryAlertsCommand;
import com.aegispulse.application.alert.result.QueryAlertsResult;
import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.domain.alert.repository.AlertRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertQueryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertQueryService alertQueryService;

    @Test
    @DisplayName("알림 목록 조회 시 기본 limit(50)을 사용한다")
    void shouldUseDefaultLimitWhenLimitIsNull() {
        given(alertRepository.findRecent(AlertState.OPEN, "svc_01", AlertType.SERVICE_5XX_RATE_HIGH, 50))
            .willReturn(
                List.of(
                    Alert.restore(
                        "alt_01",
                        AlertType.SERVICE_5XX_RATE_HIGH,
                        "svc_01",
                        AlertState.OPEN,
                        Instant.parse("2026-02-27T01:00:00Z"),
                        null,
                        "{\"observedValue\":3.1}"
                    )
                )
            );

        QueryAlertsResult result = alertQueryService.query(
            QueryAlertsCommand.builder()
                .state(AlertState.OPEN)
                .serviceId("svc_01")
                .alertType(AlertType.SERVICE_5XX_RATE_HIGH)
                .limit(null)
                .build()
        );

        assertThat(result.getAlerts()).hasSize(1);
        assertThat(result.getAlerts().getFirst().getAlertId()).isEqualTo("alt_01");
        assertThat(result.getAlerts().getFirst().getState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("limit이 범위를 벗어나면 INVALID_REQUEST 예외를 던진다")
    void shouldThrowInvalidRequestWhenLimitOutOfRange() {
        assertThatThrownBy(
            () ->
                alertQueryService.query(
                    QueryAlertsCommand.builder()
                        .state(null)
                        .serviceId(null)
                        .alertType(null)
                        .limit(201)
                        .build()
                )
        )
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            });
    }
}
