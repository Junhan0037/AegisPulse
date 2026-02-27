package com.aegispulse.application.alert.command;

import com.aegispulse.domain.alert.model.AlertState;
import com.aegispulse.domain.alert.model.AlertType;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 조회 입력 커맨드.
 */
@Getter
@Builder
public class QueryAlertsCommand {

    private final AlertState state;
    private final String serviceId;
    private final AlertType alertType;
    private final Integer limit;
}
