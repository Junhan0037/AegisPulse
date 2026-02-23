package com.aegispulse.application.consumer.command;

import com.aegispulse.domain.consumer.model.ConsumerType;
import lombok.Builder;
import lombok.Getter;

/**
 * Consumer 등록 유스케이스 입력 커맨드.
 */
@Getter
@Builder
public class RegisterConsumerCommand {

    private final String name;
    private final ConsumerType type;
}
