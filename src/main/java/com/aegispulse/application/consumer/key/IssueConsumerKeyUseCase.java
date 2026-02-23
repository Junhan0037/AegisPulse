package com.aegispulse.application.consumer.key;

import com.aegispulse.application.consumer.key.command.IssueConsumerKeyCommand;
import com.aegispulse.application.consumer.key.result.IssueConsumerKeyResult;

/**
 * Consumer API Key 발급 유스케이스.
 */
public interface IssueConsumerKeyUseCase {

    IssueConsumerKeyResult issue(IssueConsumerKeyCommand command);
}
