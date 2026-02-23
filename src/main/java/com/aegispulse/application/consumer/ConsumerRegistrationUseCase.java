package com.aegispulse.application.consumer;

import com.aegispulse.application.consumer.command.RegisterConsumerCommand;
import com.aegispulse.application.consumer.result.RegisterConsumerResult;

/**
 * Consumer 등록 유스케이스.
 */
public interface ConsumerRegistrationUseCase {

    RegisterConsumerResult register(RegisterConsumerCommand command);
}
