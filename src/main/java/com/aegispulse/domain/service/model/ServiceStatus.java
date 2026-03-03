package com.aegispulse.domain.service.model;

/**
 * 서비스 운영 상태.
 * Stage 7에서 격리 모드(ISOLATED)를 추가해 장애 전파를 차단한다.
 */
public enum ServiceStatus {
    ACTIVE,
    ISOLATED
}
