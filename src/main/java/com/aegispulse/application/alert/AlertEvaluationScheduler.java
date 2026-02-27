package com.aegispulse.application.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-007 임계치 평가 스케줄러.
 */
@Component
@RequiredArgsConstructor
public class AlertEvaluationScheduler {

    private final AlertEvaluationService alertEvaluationService;

    /**
     * 기본 1분 주기로 평가한다.
     */
    @Scheduled(fixedDelayString = "${aegispulse.alerts.evaluation-interval-ms:60000}")
    public void evaluateAlerts() {
        alertEvaluationService.evaluate();
    }
}
