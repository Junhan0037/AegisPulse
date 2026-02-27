package com.aegispulse.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.alert.command.AcknowledgeAlertCommand;
import com.aegispulse.application.alert.result.AcknowledgeAlertResult;
import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import com.aegispulse.domain.alert.repository.AlertRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertAcknowledgeServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertAcknowledgeService alertAcknowledgeService;

    @Test
    @DisplayName("OPEN 알림은 ACKED로 전이된다")
    void shouldAcknowledgeOpenAlert() {
        Alert openAlert = Alert.restore(
            "alt_01",
            AlertType.SERVICE_5XX_RATE_HIGH,
            "svc_01",
            AlertState.OPEN,
            Instant.parse("2026-02-27T00:00:00Z"),
            null,
            "{\"transition\":\"OPEN\"}"
        );
        given(alertRepository.findById("alt_01")).willReturn(Optional.of(openAlert));
        given(alertRepository.save(any(Alert.class))).willAnswer(invocation -> invocation.getArgument(0));

        AcknowledgeAlertResult result = alertAcknowledgeService.acknowledge(
            AcknowledgeAlertCommand.builder()
                .alertId("alt_01")
                .build()
        );

        assertThat(result.getAlertId()).isEqualTo("alt_01");
        assertThat(result.getState()).isEqualTo("ACKED");
        then(alertRepository).should().save(any(Alert.class));
    }

    @Test
    @DisplayName("알림이 없으면 ALERT_NOT_FOUND 예외를 던진다")
    void shouldThrowNotFoundWhenAlertDoesNotExist() {
        given(alertRepository.findById("alt_missing")).willReturn(Optional.empty());

        assertThatThrownBy(
            () -> alertAcknowledgeService.acknowledge(AcknowledgeAlertCommand.builder().alertId("alt_missing").build())
        )
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.ALERT_NOT_FOUND);
            });
    }

    @Test
    @DisplayName("OPEN이 아닌 알림은 ACK 시 ALERT_STATE_CONFLICT 예외를 던진다")
    void shouldThrowConflictWhenAlertIsNotOpen() {
        Alert ackedAlert = Alert.restore(
            "alt_acked",
            AlertType.SERVICE_5XX_RATE_HIGH,
            "svc_01",
            AlertState.ACKED,
            Instant.parse("2026-02-27T00:00:00Z"),
            null,
            "{\"transition\":\"OPEN\"}"
        );
        given(alertRepository.findById("alt_acked")).willReturn(Optional.of(ackedAlert));

        assertThatThrownBy(
            () -> alertAcknowledgeService.acknowledge(AcknowledgeAlertCommand.builder().alertId("alt_acked").build())
        )
            .isInstanceOf(AegisPulseException.class)
            .satisfies(exception -> {
                AegisPulseException aegisPulseException = (AegisPulseException) exception;
                assertThat(aegisPulseException.getErrorCode()).isEqualTo(ErrorCode.ALERT_STATE_CONFLICT);
            });
    }
}
