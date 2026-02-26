package com.aegispulse.application.consumer.key;

import com.aegispulse.application.consumer.key.command.AuthenticateConsumerKeyCommand;
import com.aegispulse.application.consumer.key.result.AuthenticateConsumerKeyResult;

/**
 * Consumer API Key 인증 유스케이스.
 */
public interface AuthenticateConsumerKeyUseCase {

    AuthenticateConsumerKeyResult authenticate(AuthenticateConsumerKeyCommand command);
}
