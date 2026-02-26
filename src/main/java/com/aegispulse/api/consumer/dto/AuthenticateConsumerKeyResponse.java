package com.aegispulse.api.consumer.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer API Key 인증 응답 DTO.
 */
@Getter
@Builder
public class AuthenticateConsumerKeyResponse {

    private final boolean authenticated;
    private final String consumerId;
    private final String keyId;
}
