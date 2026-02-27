package com.aegispulse.application.alert;

import com.aegispulse.application.alert.command.AcknowledgeAlertCommand;
import com.aegispulse.application.alert.result.AcknowledgeAlertResult;

/**
 * 알림 ACK 유스케이스.
 */
public interface AlertAcknowledgeUseCase {

    AcknowledgeAlertResult acknowledge(AcknowledgeAlertCommand command);
}
