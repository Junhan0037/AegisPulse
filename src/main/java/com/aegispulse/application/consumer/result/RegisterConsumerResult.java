package com.aegispulse.application.consumer.result;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer 등록 유스케이스 결과.
 */
@Getter
@Builder
public class RegisterConsumerResult {

    private final String consumerId;
    private final String name;
    private final String type;
}
