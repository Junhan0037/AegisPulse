package com.aegispulse.application.alert.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 단일 알림 조회 결과 모델.
 */
@Getter
@Builder
public class AlertItemResult {

    private final String alertId;
    private final String alertType;
    private final String serviceId;
    private final String state;
    private final String triggeredAt;
    private final String resolvedAt;
    private final String payload;
}
