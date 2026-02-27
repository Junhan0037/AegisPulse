package com.aegispulse.application.alert;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.alert.command.AcknowledgeAlertCommand;
import com.aegispulse.application.alert.result.AcknowledgeAlertResult;
import com.aegispulse.domain.alert.model.Alert;
import com.aegispulse.domain.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 ACK 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
public class AlertAcknowledgeService implements AlertAcknowledgeUseCase {

    private final AlertRepository alertRepository;

    @Override
    @Transactional
    public AcknowledgeAlertResult acknowledge(AcknowledgeAlertCommand command) {
        Alert alert = alertRepository.findById(command.getAlertId())
            .orElseThrow(() -> new AegisPulseException(ErrorCode.ALERT_NOT_FOUND, "요청한 알림을 찾을 수 없습니다."));

        final Alert acked;
        try {
            acked = alert.ack();
        } catch (IllegalStateException exception) {
            throw new AegisPulseException(ErrorCode.ALERT_STATE_CONFLICT, exception.getMessage());
        }

        Alert saved = alertRepository.save(acked);
        return AcknowledgeAlertResult.builder()
            .alertId(saved.getId())
            .state(saved.getState().name())
            .build();
    }
}
