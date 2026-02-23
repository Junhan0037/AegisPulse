package com.aegispulse.application.consumer.key.result;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer API Key 발급 유스케이스 결과.
 */
@Getter
@Builder
public class IssueConsumerKeyResult {

    private final String keyId;
    private final String apiKey;
}
