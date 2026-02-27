package com.aegispulse.application.alert.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 알림 ACK 결과.
 */
@Getter
@Builder
public class AcknowledgeAlertResult {

    private final String alertId;
    private final String state;
}
