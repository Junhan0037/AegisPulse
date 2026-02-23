package com.aegispulse.api.consumer.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer API Key 발급 응답 DTO.
 */
@Getter
@Builder
public class IssueConsumerKeyResponse {

    private final String keyId;
    private final String apiKey;
}
