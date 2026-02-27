package com.aegispulse.api.alert.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 알림 ACK 응답 DTO.
 */
@Getter
@Builder
public class AcknowledgeAlertResponse {

    private final String alertId;
    private final String state;
}
