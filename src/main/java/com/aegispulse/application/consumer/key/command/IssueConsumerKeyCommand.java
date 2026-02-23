package com.aegispulse.application.consumer.key.command;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer API Key 발급 유스케이스 입력 커맨드.
 */
@Getter
@Builder
public class IssueConsumerKeyCommand {

    private final String consumerId;
}
