package com.aegispulse.application.consumer.key;

/**
 * API Key 해시기 추상화.
 */
public interface ApiKeyHasher {

    String hash(String plainKey);

    /**
     * 평문 키와 저장된 해시 문자열의 일치 여부를 검증한다.
     * 해시 포맷이 올바르지 않거나 매칭되지 않으면 false를 반환한다.
     */
    boolean matches(String plainKey, String hashedKey);
}
