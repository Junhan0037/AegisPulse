package com.aegispulse.application.alert;

import com.aegispulse.application.alert.command.QueryAlertsCommand;
import com.aegispulse.application.alert.result.QueryAlertsResult;

/**
 * 알림 조회 유스케이스.
 */
public interface AlertQueryUseCase {

    QueryAlertsResult query(QueryAlertsCommand command);
}
