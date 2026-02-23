package com.aegispulse.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Pbkdf2ApiKeyHasherTest {

    private final Pbkdf2ApiKeyHasher hasher = new Pbkdf2ApiKeyHasher();

    @Test
    @DisplayName("동일 키를 해시해도 salt가 달라 서로 다른 해시 문자열을 생성한다")
    void shouldGenerateDifferentHashesForSamePlainKey() {
        String plainKey = "ak_same_plain_key";

        String hashOne = hasher.hash(plainKey);
        String hashTwo = hasher.hash(plainKey);

        assertThat(hashOne).startsWith("pbkdf2$");
        assertThat(hashTwo).startsWith("pbkdf2$");
        assertThat(hashOne).isNotEqualTo(hashTwo);
        assertThat(hashOne).doesNotContain(plainKey);
        assertThat(hashTwo).doesNotContain(plainKey);
        assertThat(hashOne.split("\\$")).hasSize(4);
    }

    @Test
    @DisplayName("빈 키는 해시할 수 없다")
    void shouldRejectBlankPlainKey() {
        assertThatThrownBy(() -> hasher.hash("  "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
