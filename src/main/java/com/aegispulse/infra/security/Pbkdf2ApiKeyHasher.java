package com.aegispulse.infra.security;

import com.aegispulse.application.consumer.key.ApiKeyHasher;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
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

    @Override
    public boolean matches(String plainKey, String hashedKey) {
        if (!StringUtils.hasText(plainKey) || !StringUtils.hasText(hashedKey)) {
            return false;
        }

        String[] tokens = hashedKey.split("\\$");
        if (tokens.length != 4 || !"pbkdf2".equals(tokens[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(tokens[1]);
            byte[] salt = Base64.getUrlDecoder().decode(tokens[2]);
            byte[] expectedHash = Base64.getUrlDecoder().decode(tokens[3]);
            if (iterations < 1 || expectedHash.length == 0) {
                return false;
            }

            PBEKeySpec spec = new PBEKeySpec(plainKey.toCharArray(), salt, iterations, expectedHash.length * 8);
            try {
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
                byte[] actualHash = keyFactory.generateSecret(spec).getEncoded();
                // 해시 비교는 타이밍 공격 완화를 위해 상수 시간 비교를 사용한다.
                return MessageDigest.isEqual(expectedHash, actualHash);
            } finally {
                spec.clearPassword();
            }
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            return false;
        }
    }
}
