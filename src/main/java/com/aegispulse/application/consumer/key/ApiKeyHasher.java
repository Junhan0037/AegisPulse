package com.aegispulse.application.consumer.key;

/**
 * API Key 해시기 추상화.
 */
public interface ApiKeyHasher {

    String hash(String plainKey);
}
