package com.aegispulse.api.alert.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 알림 단건 응답 DTO.
 */
@Getter
@Builder
public class AlertItemResponse {

    private final String alertId;
    private final String alertType;
    private final String serviceId;
    private final String state;
    private final String triggeredAt;
    private final String resolvedAt;
    private final String payload;
}
