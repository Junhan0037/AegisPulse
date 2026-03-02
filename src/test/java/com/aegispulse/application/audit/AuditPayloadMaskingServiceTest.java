package com.aegispulse.application.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditPayloadMaskingServiceTest {

    private final AuditPayloadMaskingService auditPayloadMaskingService = new AuditPayloadMaskingService(new ObjectMapper());

    @Test
    @DisplayName("중첩 JSON의 민감 키를 모두 마스킹한다")
    void shouldMaskSensitiveKeysRecursively() {
        String source =
            """
            {
              "apiKey": "abcd",
              "profile": {
                "authorization": "Bearer x.y.z",
                "password": "pw1234",
                "nested": [
                  {"token": "t-1"},
                  {"safe": "ok"}
                ]
              }
            }
            """;

        String masked = auditPayloadMaskingService.mask(source);

        assertThat(masked).contains("\"apiKey\":\"***\"");
        assertThat(masked).contains("\"authorization\":\"***\"");
        assertThat(masked).contains("\"password\":\"***\"");
        assertThat(masked).contains("\"token\":\"***\"");
        assertThat(masked).contains("\"safe\":\"ok\"");
    }

    @Test
    @DisplayName("유효하지 않은 JSON이면 원문 노출 대신 빈 JSON으로 대체한다")
    void shouldReturnEmptyJsonWhenPayloadIsInvalidJson() {
        String masked = auditPayloadMaskingService.mask("not-json-payload");

        assertThat(masked).isEqualTo("{}");
    }
}
