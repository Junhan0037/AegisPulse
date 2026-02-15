package com.aegispulse.domain.service.model;

/**
 * 서비스가 소속된 배포 환경.
 * PRD FR-001 규약(DEV|STAGE|PROD)을 그대로 사용한다.
 */
public enum ServiceEnvironment {
    DEV,
    STAGE,
    PROD
}
