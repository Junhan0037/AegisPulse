package com.aegispulse.domain.policy.model;

/**
 * 템플릿 정책에서 요구하는 인증 방식 타입.
 */
public enum AuthType {
    NONE,
    API_KEY_REQUIRED,
    JWT_REQUIRED
}
