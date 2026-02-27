package com.aegispulse.application.alert.result;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 조회 결과.
 */
@Getter
@Builder
public class QueryAlertsResult {

    private final List<AlertItemResult> alerts;
}
