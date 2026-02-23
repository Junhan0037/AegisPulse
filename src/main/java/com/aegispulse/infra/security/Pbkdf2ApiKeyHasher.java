package com.aegispulse.infra.security;

import com.aegispulse.application.consumer.key.ApiKeyHasher;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * PBKDF2 기반 API Key 해시기.
 */
@Component
public class Pbkdf2ApiKeyHasher implements ApiKeyHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String hash(String plainKey) {
        if (!StringUtils.hasText(plainKey)) {
            throw new IllegalArgumentException("plainKey는 비어 있을 수 없습니다.");
        }

        byte[] salt = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(plainKey.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hashBytes = keyFactory.generateSecret(spec).getEncoded();
            return "pbkdf2$"
                + ITERATIONS
                + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
                + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("API Key 해시 생성에 실패했습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
