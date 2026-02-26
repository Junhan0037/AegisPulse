package com.aegispulse.application.consumer.key.command;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer API Key 인증 유스케이스 입력 커맨드.
 */
@Getter
@Builder
public class AuthenticateConsumerKeyCommand {

    private final String serviceId;
    private final String routeId;
    private final String consumerId;
    private final String apiKey;
}
