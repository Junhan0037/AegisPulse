package com.aegispulse.application.alert.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 알림 ACK 입력 커맨드.
 */
@Getter
@Builder
public class AcknowledgeAlertCommand {

    private final String alertId;
}
