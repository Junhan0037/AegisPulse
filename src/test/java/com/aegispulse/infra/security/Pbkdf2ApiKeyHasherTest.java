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

    @Test
    @DisplayName("일치하는 평문 키면 true를 반환한다")
    void shouldReturnTrueWhenPlainKeyMatchesHash() {
        String plainKey = "ak_match_key";
        String hashedKey = hasher.hash(plainKey);

        boolean matched = hasher.matches(plainKey, hashedKey);

        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("평문 키가 다르면 false를 반환한다")
    void shouldReturnFalseWhenPlainKeyDoesNotMatchHash() {
        String hashedKey = hasher.hash("ak_original_key");

        boolean matched = hasher.matches("ak_other_key", hashedKey);

        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("해시 포맷이 손상되면 false를 반환한다")
    void shouldReturnFalseWhenHashFormatIsMalformed() {
        boolean matched = hasher.matches("ak_plain_key", "pbkdf2$not-a-number$salt$hash");

        assertThat(matched).isFalse();
    }
}
