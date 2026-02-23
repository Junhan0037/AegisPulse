package com.aegispulse.infra.security;

import com.aegispulse.application.consumer.key.ApiKeyGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * SecureRandom 기반 API Key 원문 생성기.
 */
@Component
public class SecureRandomApiKeyGenerator implements ApiKeyGenerator {

    private static final int KEY_BYTE_LENGTH = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String generate() {
        byte[] randomBytes = new byte[KEY_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "ak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
