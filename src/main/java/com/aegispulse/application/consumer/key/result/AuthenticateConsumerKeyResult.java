package com.aegispulse.application.consumer.key.result;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer API Key 인증 유스케이스 결과.
 */
@Getter
@Builder
public class AuthenticateConsumerKeyResult {

    private final boolean authenticated;
    private final String consumerId;
    private final String keyId;
}
