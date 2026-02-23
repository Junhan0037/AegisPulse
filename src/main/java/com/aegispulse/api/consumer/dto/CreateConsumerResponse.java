package com.aegispulse.api.consumer.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Consumer 생성 응답 DTO.
 */
@Getter
@Builder
public class CreateConsumerResponse {

    private final String consumerId;
    private final String name;
    private final String type;
}
