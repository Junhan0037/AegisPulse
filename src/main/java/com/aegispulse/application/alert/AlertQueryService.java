package com.aegispulse.application.alert;

import com.aegispulse.api.common.exception.AegisPulseException;
import com.aegispulse.api.common.exception.ErrorCode;
import com.aegispulse.application.alert.command.QueryAlertsCommand;
import com.aegispulse.application.alert.result.AlertItemResult;
import com.aegispulse.application.alert.result.QueryAlertsResult;
import com.aegispulse.domain.alert.repository.AlertRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 알림 조회 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
public class AlertQueryService implements AlertQueryUseCase {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public QueryAlertsResult query(QueryAlertsCommand command) {
        int limit = resolveLimit(command.getLimit());
        String serviceId = normalizeOptional(command.getServiceId());

        List<AlertItemResult> alerts = alertRepository
            .findRecent(command.getState(), serviceId, command.getAlertType(), limit)
            .stream()
            .map(
                alert -> AlertItemResult.builder()
                    .alertId(alert.getId())
                    .alertType(alert.getAlertType().name())
                    .serviceId(alert.getTargetId())
                    .state(alert.getState().name())
                    .triggeredAt(alert.getTriggeredAt().toString())
                    .resolvedAt(alert.getResolvedAt() == null ? null : alert.getResolvedAt().toString())
                    .payload(alert.getPayload())
                    .build()
            )
            .toList();

        return QueryAlertsResult.builder()
            .alerts(alerts)
            .build();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new AegisPulseException(ErrorCode.INVALID_REQUEST, "limit은 1~200 범위여야 합니다.");
        }
        return limit;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
