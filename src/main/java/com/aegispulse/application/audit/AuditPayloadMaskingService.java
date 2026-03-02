package com.aegispulse.application.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 감사로그 before/after JSON의 민감정보를 마스킹한다.
 */
@Component
@RequiredArgsConstructor
public class AuditPayloadMaskingService {

    private static final String MASKED_VALUE = "***";
    private static final Set<String> SENSITIVE_TOKENS = Set.of("key", "token", "secret", "password", "authorization");

    private final ObjectMapper objectMapper;

    /**
     * JSON 문자열을 순회하며 민감 키 값을 마스킹한다.
     * 파싱 실패 시 원문 노출을 막기 위해 빈 JSON으로 대체한다.
     */
    public String mask(String jsonPayload) {
        if (!StringUtils.hasText(jsonPayload)) {
            return "{}";
        }
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            JsonNode masked = maskNode(root);
            return objectMapper.writeValueAsString(masked);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private JsonNode maskNode(JsonNode node) {
        if (node == null) {
            return objectMapper.createObjectNode();
        }

        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode masked = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitiveKey(entry.getKey())) {
                    masked.put(entry.getKey(), MASKED_VALUE);
                } else {
                    masked.set(entry.getKey(), maskNode(entry.getValue()));
                }
            }
            return masked;
        }

        if (node.isArray()) {
            ArrayNode masked = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                masked.add(maskNode(element));
            }
            return masked;
        }

        return node.deepCopy();
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_TOKENS.stream().anyMatch(normalized::contains);
    }
}
